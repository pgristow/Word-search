package com.wordsearch.service

import com.wordsearch.dto.WordInfo
import com.wordsearch.model.BossLevelAttempt
import com.wordsearch.model.BossType
import com.wordsearch.repository.BossLevelAttemptRepository
import com.wordsearch.repository.CategoryRepository
import com.wordsearch.repository.WordRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

@Service
class BossLevelService(
    private val bossLevelAttemptRepository: BossLevelAttemptRepository,
    private val gameBoardGenerator: GameBoardGenerator,
    private val categoryRepository: CategoryRepository,
    private val wordRepository: WordRepository
) {

    /**
     * Determines if current level is a boss level
     */
    fun isBossLevel(level: Int): Boolean {
        return level % 5 == 0
    }

    /**
     * Determines the boss type based on level
     */
    fun getBossType(level: Int): BossType {
        return when {
            level % 30 == 0 -> BossType.CHAOS
            level % 20 == 0 -> BossType.REVERSE
            level % 10 == 0 -> BossType.MEGA
            else -> BossType.SPEED
        }
    }

    /**
     * Generates a boss level challenge
     */
    @Transactional
    fun startBossLevel(
        userId: UUID,
        sessionId: UUID,
        level: Int,
        categoryId: UUID
    ): BossLevelResponse {
        if (!isBossLevel(level)) {
            throw IllegalArgumentException("Level $level is not a boss level")
        }

        val bossType = getBossType(level)
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { IllegalArgumentException("Category not found") }

        // Get boss level configuration
        val config = getBossLevelConfig(level, bossType)

        // Generate boss board
        val words = wordRepository.findByCategoryId(categoryId).map { it.word }
        val gameBoard = generateBossBoard(level, bossType, words, category.name, config)

        // Create boss level attempt record
        val attempt = BossLevelAttempt(
            userId = userId,
            sessionId = sessionId,
            bossLevel = level,
            bossType = bossType,
            wordsRequired = config.wordsRequired,
            timeLimitSeconds = config.timeLimitSeconds,
            shufflesOccurred = 0,
            completed = false
        )
        val savedAttempt = bossLevelAttemptRepository.save(attempt)

        return BossLevelResponse(
            attemptId = savedAttempt.id.toString(),
            bossLevel = level,
            bossType = bossType.name,
            gridSize = gameBoard.gridSize,
            category = category.name,
            grid = gameBoard.grid.map { it.joinToString("") },
            words = gameBoard.placedWords.map { pw ->
                WordInfo(
                    word = pw.word,
                    isReversed = pw.isReversed,
                    startRow = pw.startRow,
                    startCol = pw.startCol,
                    direction = pw.direction.name
                )
            },
            wordsRequired = config.wordsRequired,
            timeLimitSeconds = config.timeLimitSeconds,
            shuffleIntervalSeconds = config.shuffleIntervalSeconds,
            instructions = getBossInstructions(bossType, config),
            startTime = LocalDateTime.now().toString()
        )
    }

    /**
     * Handles board shuffle for boss levels
     */
    @Transactional
    fun shuffleBoard(
        attemptId: UUID,
        userId: UUID,
        foundWords: List<String>
    ): ShuffleResponse {
        val attempt = bossLevelAttemptRepository.findById(attemptId)
            .orElseThrow { IllegalArgumentException("Boss level attempt not found") }

        if (attempt.userId != userId) {
            throw IllegalArgumentException("Unauthorized")
        }

        if (attempt.completed) {
            throw IllegalArgumentException("Boss level already completed")
        }

        // Get category and words
        val category = categoryRepository.findAll().first() // Simplified for now
        val allWords = wordRepository.findByCategoryId(category.id).map { it.word }

        // Filter out found words and regenerate board
        val remainingWords = allWords.filter { it !in foundWords }
        val config = getBossLevelConfig(attempt.bossLevel, attempt.bossType)
        val newBoard = generateBossBoard(
            level = attempt.bossLevel,
            bossType = attempt.bossType,
            words = remainingWords,
            categoryName = category.name,
            config = config
        )

        // Update attempt with shuffle count
        val updatedAttempt = attempt.copy(
            shufflesOccurred = attempt.shufflesOccurred + 1
        )
        bossLevelAttemptRepository.save(updatedAttempt)

        return ShuffleResponse(
            shuffleCount = updatedAttempt.shufflesOccurred,
            grid = newBoard.grid.map { it.joinToString("") },
            words = newBoard.placedWords.map { pw ->
                WordInfo(
                    word = pw.word,
                    isReversed = pw.isReversed,
                    startRow = pw.startRow,
                    startCol = pw.startCol,
                    direction = pw.direction.name
                )
            },
            message = "Board shuffled! Found words cleared. ${config.timeLimitSeconds} seconds remaining."
        )
    }

    /**
     * Completes a boss level attempt
     */
    @Transactional
    fun completeBossLevel(
        attemptId: UUID,
        userId: UUID,
        wordsFound: Int,
        timeTaken: Int
    ): BossCompletionResponse {
        val attempt = bossLevelAttemptRepository.findById(attemptId)
            .orElseThrow { IllegalArgumentException("Boss level attempt not found") }

        if (attempt.userId != userId) {
            throw IllegalArgumentException("Unauthorized")
        }

        val success = wordsFound >= attempt.wordsRequired
        val baseReward = when (attempt.bossType) {
            BossType.SPEED -> 500
            BossType.MEGA -> 1000
            BossType.REVERSE -> 750
            BossType.CHAOS -> 2000
        }

        // Calculate bonus based on performance
        val timeBonus = if (success) {
            val percentRemaining = ((attempt.timeLimitSeconds - timeTaken).toFloat() / attempt.timeLimitSeconds)
            (baseReward * percentRemaining * 0.5).toInt()
        } else 0

        val shufflePenalty = attempt.shufflesOccurred * 50
        val finalScore = maxOf(0, baseReward + timeBonus - shufflePenalty)

        // Update attempt
        val updatedAttempt = attempt.copy(
            completed = true,
            wordsFound = wordsFound,
            timeTakenSeconds = timeTaken,
            scoreEarned = if (success) finalScore else 0
        )
        bossLevelAttemptRepository.save(updatedAttempt)

        return BossCompletionResponse(
            success = success,
            wordsFound = wordsFound,
            wordsRequired = attempt.wordsRequired,
            timeTaken = timeTaken,
            timeLimit = attempt.timeLimitSeconds,
            shufflesUsed = attempt.shufflesOccurred,
            baseReward = if (success) baseReward else 0,
            timeBonus = timeBonus,
            shufflePenalty = shufflePenalty,
            totalScore = if (success) finalScore else 0,
            message = if (success) "Boss Level Defeated! 🏆" else "Boss Level Failed. Try again!",
            nextBossLevel = if (success) attempt.bossLevel + 5 else attempt.bossLevel
        )
    }

    /**
     * Get boss level statistics
     */
    fun getBossStatistics(userId: UUID): BossStatistics {
        val allAttempts = bossLevelAttemptRepository.findAll()
            .filter { it.userId == userId }

        val completedAttempts = allAttempts.filter { it.completed && it.wordsFound >= it.wordsRequired }

        return BossStatistics(
            totalBossAttempts = allAttempts.size,
            totalBossCompleted = completedAttempts.size,
            totalBossFailed = allAttempts.count { it.completed && it.wordsFound < it.wordsRequired },
            highestBossLevel = completedAttempts.maxOfOrNull { it.bossLevel } ?: 0,
            totalBossScore = completedAttempts.sumOf { it.scoreEarned },
            averageCompletionTime = if (completedAttempts.isNotEmpty()) {
                completedAttempts.mapNotNull { it.timeTakenSeconds }.average().toInt()
            } else 0,
            bossTypeStats = BossType.values().map { type ->
                BossTypeStats(
                    type = type.name,
                    attempted = allAttempts.count { it.bossType == type },
                    completed = completedAttempts.count { it.bossType == type }
                )
            }
        )
    }

    // Private helper methods

    private fun getBossLevelConfig(level: Int, bossType: BossType): BossLevelConfig {
        return when (bossType) {
            BossType.SPEED -> BossLevelConfig(
                wordsRequired = 10,
                timeLimitSeconds = 45,
                shuffleIntervalSeconds = null,
                gridSizeModifier = 0
            )
            BossType.MEGA -> BossLevelConfig(
                wordsRequired = 15,
                timeLimitSeconds = 90,
                shuffleIntervalSeconds = null,
                gridSizeModifier = 5
            )
            BossType.REVERSE -> BossLevelConfig(
                wordsRequired = 12,
                timeLimitSeconds = 60,
                shuffleIntervalSeconds = null,
                gridSizeModifier = 0
            )
            BossType.CHAOS -> BossLevelConfig(
                wordsRequired = 15,
                timeLimitSeconds = 120,
                shuffleIntervalSeconds = 30,
                gridSizeModifier = 0
            )
        }
    }

    private fun generateBossBoard(
        level: Int,
        bossType: BossType,
        words: List<String>,
        categoryName: String,
        config: BossLevelConfig
    ): GameBoard {
        val diffConfig = gameBoardGenerator.getDifficultyConfig(level)
        val adjustedGridSize = diffConfig.gridSize + config.gridSizeModifier

        // Override reverse probability for REVERSE boss
        val reverseProb = if (bossType == BossType.REVERSE) 1.0f else diffConfig.reverseWordProbability

        // Generate board with boss-specific settings
        val grid = Array(adjustedGridSize) { CharArray(adjustedGridSize) { ' ' } }
        val placedWords = mutableListOf<PlacedWord>()

        val targetWords = words.shuffled().take(config.wordsRequired + 3) // Extra words for difficulty

        for (word in targetWords) {
            val shouldReverse = Random.nextFloat() < reverseProb
            val wordToPlace = if (shouldReverse) word.reversed() else word

            var placed = false
            repeat(100) {
                if (placed) return@repeat

                val row = Random.nextInt(adjustedGridSize)
                val col = Random.nextInt(adjustedGridSize)
                val direction = diffConfig.allowedDirections.random()

                if (canPlaceWord(grid, wordToPlace, row, col, direction)) {
                    placeWordOnGrid(grid, wordToPlace, row, col, direction)
                    placedWords.add(
                        PlacedWord(
                            word = word,
                            displayWord = wordToPlace,
                            isReversed = shouldReverse,
                            startRow = row,
                            startCol = col,
                            direction = direction
                        )
                    )
                    placed = true
                }
            }
        }

        // Fill empty cells
        fillEmptyCells(grid, diffConfig.distractorLetters)

        return GameBoard(grid, placedWords.take(config.wordsRequired), level, adjustedGridSize, categoryName)
    }

    private fun canPlaceWord(
        grid: Array<CharArray>,
        word: String,
        row: Int,
        col: Int,
        direction: Direction
    ): Boolean {
        val (dr, dc) = direction.deltas

        for (i in word.indices) {
            val r = row + dr * i
            val c = col + dc * i

            if (r !in grid.indices || c !in grid[r].indices) return false
            if (grid[r][c] != ' ' && grid[r][c] != word[i]) return false
        }
        return true
    }

    private fun placeWordOnGrid(
        grid: Array<CharArray>,
        word: String,
        row: Int,
        col: Int,
        direction: Direction
    ) {
        val (dr, dc) = direction.deltas
        for (i in word.indices) {
            grid[row + dr * i][col + dc * i] = word[i]
        }
    }

    private fun fillEmptyCells(grid: Array<CharArray>, letters: String) {
        for (r in grid.indices) {
            for (c in grid[r].indices) {
                if (grid[r][c] == ' ') {
                    grid[r][c] = letters.random()
                }
            }
        }
    }

    private fun getBossInstructions(bossType: BossType, config: BossLevelConfig): String {
        return when (bossType) {
            BossType.SPEED -> "⚡ SPEED BOSS: Find ${config.wordsRequired} words in ${config.timeLimitSeconds} seconds!"
            BossType.MEGA -> "🎯 MEGA BOSS: Find ${config.wordsRequired} words on a LARGER grid in ${config.timeLimitSeconds} seconds!"
            BossType.REVERSE -> "🔄 REVERSE BOSS: ALL words are backwards! Find ${config.wordsRequired} reversed words!"
            BossType.CHAOS -> "💥 CHAOS BOSS: Board shuffles every ${config.shuffleIntervalSeconds} seconds! Find ${config.wordsRequired} words!"
        }
    }
}

// DTOs
data class BossLevelConfig(
    val wordsRequired: Int,
    val timeLimitSeconds: Int,
    val shuffleIntervalSeconds: Int?,
    val gridSizeModifier: Int
)

data class BossLevelResponse(
    val attemptId: String,
    val bossLevel: Int,
    val bossType: String,
    val gridSize: Int,
    val category: String,
    val grid: List<String>,
    val words: List<WordInfo>,
    val wordsRequired: Int,
    val timeLimitSeconds: Int,
    val shuffleIntervalSeconds: Int?,
    val instructions: String,
    val startTime: String
)

data class ShuffleResponse(
    val shuffleCount: Int,
    val grid: List<String>,
    val words: List<WordInfo>,
    val message: String
)

data class BossCompletionResponse(
    val success: Boolean,
    val wordsFound: Int,
    val wordsRequired: Int,
    val timeTaken: Int,
    val timeLimit: Int,
    val shufflesUsed: Int,
    val baseReward: Int,
    val timeBonus: Int,
    val shufflePenalty: Int,
    val totalScore: Int,
    val message: String,
    val nextBossLevel: Int
)

data class BossStatistics(
    val totalBossAttempts: Int,
    val totalBossCompleted: Int,
    val totalBossFailed: Int,
    val highestBossLevel: Int,
    val totalBossScore: Int,
    val averageCompletionTime: Int,
    val bossTypeStats: List<BossTypeStats>
)

data class BossTypeStats(
    val type: String,
    val attempted: Int,
    val completed: Int
)
