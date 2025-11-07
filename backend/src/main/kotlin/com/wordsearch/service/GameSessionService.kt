package com.wordsearch.service

import com.wordsearch.model.*
import com.wordsearch.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class GameSessionService(
    private val gameSessionRepository: GameSessionRepository,
    private val userProgressRepository: UserProgressRepository,
    private val gameBoardGenerator: GameBoardGenerator,
    private val categoryRepository: CategoryRepository,
    private val wordRepository: WordRepository
) {

    @Transactional
    fun startNewSession(userId: UUID, categoryId: UUID, gameMode: GameMode = GameMode.CLASSIC): GameSessionResponse {
        // End any active sessions for this user (only for classic mode)
        if (gameMode == GameMode.CLASSIC) {
            val activeSession = gameSessionRepository.findByUserIdAndIsActive(userId, true)
            if (activeSession != null) {
                endSession(activeSession.id, userId)
            }
        }

        // Get or create user progress
        val userProgress = userProgressRepository.findByUserId(userId)
            ?: userProgressRepository.save(UserProgress(userId = userId))

        // Get category
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { IllegalArgumentException("Category not found") }

        // Create new game session
        val session = GameSession(
            userId = userId,
            startingLevel = userProgress.currentLevel,
            sessionStart = LocalDateTime.now(),
            gameMode = gameMode
        )
        val savedSession = gameSessionRepository.save(session)

        // Generate game board
        val words = wordRepository.findByCategoryId(categoryId).map { it.word }
        val gameBoard = gameBoardGenerator.generateBoard(
            level = userProgress.currentLevel,
            words = words,
            categoryName = category.name
        )

        return GameSessionResponse(
            sessionId = savedSession.id.toString(),
            level = userProgress.currentLevel,
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
            currentScore = 0,
            currentCombo = 0,
            totalWordsFound = 0,
            gameMode = gameMode.name
        )
    }

    @Transactional
    fun submitWord(
        sessionId: UUID,
        userId: UUID,
        word: String,
        isReversed: Boolean,
        isDiagonal: Boolean,
        timeElapsed: Int
    ): WordSubmissionResponse {
        val session = gameSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        if (session.userId != userId) {
            throw IllegalArgumentException("Unauthorized")
        }

        if (!session.isActive) {
            throw IllegalArgumentException("Session is not active")
        }

        // Mode-specific scoring
        val score: Int
        val currentCombo: Int

        if (session.gameMode == GameMode.CASUAL) {
            // Casual mode: Simple scoring, no combos or time bonuses
            score = calculateCasualScore(word)
            currentCombo = 0
        } else {
            // Classic mode: Full competitive scoring
            currentCombo = session.highestCombo + 1
            score = gameBoardGenerator.calculateWordScore(
                word = word,
                isReversed = isReversed,
                isDiagonal = isDiagonal,
                timeElapsed = timeElapsed,
                currentCombo = currentCombo
            )
        }

        // Update session
        val updatedSession = session.copy(
            totalScore = session.totalScore + score,
            wordsFound = session.wordsFound + 1,
            highestCombo = maxOf(session.highestCombo, currentCombo)
        )
        gameSessionRepository.save(updatedSession)

        // Update user progress
        val userProgress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("User progress not found")

        val updatedProgress = userProgress.copy(
            totalScore = userProgress.totalScore + score,
            totalWordsFound = userProgress.totalWordsFound + 1,
            totalReversedWordsFound = if (isReversed) userProgress.totalReversedWordsFound + 1
                                      else userProgress.totalReversedWordsFound,
            highestCombo = maxOf(userProgress.highestCombo, currentCombo),
            lastPlayedAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        userProgressRepository.save(updatedProgress)

        // Check for level up
        val newLevel = gameBoardGenerator.calculateLevel(updatedProgress.totalScore)
        val leveledUp = newLevel > updatedProgress.currentLevel

        if (leveledUp) {
            val progressWithNewLevel = updatedProgress.copy(
                currentLevel = newLevel,
                highestLevelReached = maxOf(updatedProgress.highestLevelReached, newLevel)
            )
            userProgressRepository.save(progressWithNewLevel)
        }

        return WordSubmissionResponse(
            valid = true,
            score = score,
            totalScore = updatedProgress.totalScore + score,
            currentCombo = currentCombo,
            leveledUp = leveledUp,
            newLevel = if (leveledUp) newLevel else null,
            wordsFoundInSession = updatedSession.wordsFound
        )
    }

    @Transactional
    fun endSession(sessionId: UUID, userId: UUID): SessionSummary {
        val session = gameSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        if (session.userId != userId) {
            throw IllegalArgumentException("Unauthorized")
        }

        val updatedSession = session.copy(
            isActive = false,
            sessionEnd = LocalDateTime.now(),
            endingLevel = gameBoardGenerator.calculateLevel(session.totalScore.toLong())
        )
        gameSessionRepository.save(updatedSession)

        // Update user progress for casual mode puzzle completion
        if (session.gameMode == GameMode.CASUAL) {
            val userProgress = userProgressRepository.findByUserId(userId)
                ?: throw IllegalArgumentException("User progress not found")

            val updatedProgress = userProgress.copy(
                casualPuzzlesCompleted = userProgress.casualPuzzlesCompleted + 1,
                lastPlayedAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            userProgressRepository.save(updatedProgress)
        }

        return SessionSummary(
            sessionId = sessionId.toString(),
            startingLevel = session.startingLevel,
            endingLevel = updatedSession.endingLevel ?: session.startingLevel,
            totalScore = session.totalScore,
            wordsFound = session.wordsFound,
            highestCombo = session.highestCombo,
            duration = java.time.Duration.between(session.sessionStart, updatedSession.sessionEnd).toMinutes()
        )
    }

    fun getActiveSession(userId: UUID): GameSessionResponse? {
        val session = gameSessionRepository.findByUserIdAndIsActive(userId, true)
            ?: return null

        val userProgress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("User progress not found")

        return GameSessionResponse(
            sessionId = session.id.toString(),
            level = userProgress.currentLevel,
            gridSize = 10, // Would need to regenerate board or store it
            category = "Unknown", // Would need to track category in session
            grid = emptyList(),
            words = emptyList(),
            currentScore = session.totalScore,
            currentCombo = session.highestCombo,
            totalWordsFound = session.wordsFound,
            gameMode = session.gameMode.name
        )
    }

    @Transactional
    fun saveCasualProgress(sessionId: UUID, userId: UUID): SessionSummary {
        val session = gameSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        if (session.userId != userId) {
            throw IllegalArgumentException("Unauthorized")
        }

        if (session.gameMode != GameMode.CASUAL) {
            throw IllegalArgumentException("Save/resume is only available for casual mode")
        }

        val updatedSession = session.copy(
            isPaused = true,
            isActive = false
        )
        gameSessionRepository.save(updatedSession)

        return SessionSummary(
            sessionId = sessionId.toString(),
            startingLevel = session.startingLevel,
            endingLevel = session.startingLevel, // Level doesn't change in casual
            totalScore = session.totalScore,
            wordsFound = session.wordsFound,
            highestCombo = 0, // No combos in casual
            duration = java.time.Duration.between(session.sessionStart, LocalDateTime.now()).toMinutes()
        )
    }

    @Transactional
    fun resumeCasualGame(sessionId: UUID, userId: UUID): GameSessionResponse? {
        val session = gameSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        if (session.userId != userId) {
            throw IllegalArgumentException("Unauthorized")
        }

        if (session.gameMode != GameMode.CASUAL) {
            throw IllegalArgumentException("Resume is only available for casual mode")
        }

        if (!session.isPaused) {
            throw IllegalArgumentException("Session is not paused")
        }

        val updatedSession = session.copy(
            isPaused = false,
            isActive = true
        )
        gameSessionRepository.save(updatedSession)

        // Return active session response
        return getActiveSession(userId)
    }

    /**
     * Calculate score for casual mode
     * Simple scoring: 100 points per character in the word
     * No bonuses for reversed, diagonal, speed, or combos
     */
    private fun calculateCasualScore(word: String): Int {
        return 100 * word.length
    }
}

// DTOs
data class GameSessionResponse(
    val sessionId: String,
    val level: Int,
    val gridSize: Int,
    val category: String,
    val grid: List<String>,
    val words: List<WordInfo>,
    val currentScore: Int,
    val currentCombo: Int,
    val totalWordsFound: Int,
    val gameMode: String = "CLASSIC"
)

data class WordInfo(
    val word: String,
    val isReversed: Boolean,
    val startRow: Int,
    val startCol: Int,
    val direction: String
)

data class WordSubmissionResponse(
    val valid: Boolean,
    val score: Int,
    val totalScore: Long,
    val currentCombo: Int,
    val leveledUp: Boolean,
    val newLevel: Int?,
    val wordsFoundInSession: Int
)

data class SessionSummary(
    val sessionId: String,
    val startingLevel: Int,
    val endingLevel: Int,
    val totalScore: Int,
    val wordsFound: Int,
    val highestCombo: Int,
    val duration: Long
)
