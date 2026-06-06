package com.wordsearch.service

import com.wordsearch.model.*
import com.wordsearch.repository.*
import com.wordsearch.dto.CellDto
import com.wordsearch.dto.GameSessionResponse
import com.wordsearch.dto.WordInfo
import com.wordsearch.dto.WordSubmissionResponse
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
    private val wordRepository: WordRepository,
    private val userFoundWordRepository: UserFoundWordRepository,
    private val scoringService: ScoringService,
    private val wordClassifier: WordClassifier,
    private val objectMapper: com.fasterxml.jackson.databind.ObjectMapper
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

        // Generate game board
        val words = wordRepository.findByCategoryId(categoryId).map { it.word }
        val gameBoard = gameBoardGenerator.generateBoard(
            level = userProgress.currentLevel,
            words = words,
            categoryName = category.name
        )

        // Create new game session, persisting the board for server-side validation
        val session = GameSession(
            userId = userId,
            categoryId = categoryId,
            startingLevel = userProgress.currentLevel,
            sessionStart = LocalDateTime.now(),
            gameMode = gameMode,
            boardState = objectMapper.writeValueAsString(gameBoard.toBoardState())
        )
        val savedSession = gameSessionRepository.save(session)

        return GameSessionResponse(
            sessionId = savedSession.id.toString(),
            level = userProgress.currentLevel,
            gridSize = gameBoard.gridSize,
            category = category.name,
            grid = gameBoard.grid.map { it.toList() },
            words = gameBoard.placedWords.map { pw ->
                WordInfo(
                    word = pw.word,
                    isReversed = pw.isReversed,
                    startRow = pw.startRow,
                    startCol = pw.startCol,
                    direction = pw.direction.name
                )
            },
            targetWordCount = gameBoard.placedWords.size,
            currentScore = 0,
            wordsFound = 0,
            currentCombo = 0,
            gameMode = gameMode.name,
            foundWords = emptyList() // New session, no words found yet
        )
    }

    @Transactional
    fun submitWord(
        sessionId: UUID,
        userId: UUID,
        word: String,
        isReversed: Boolean,
        isDiagonal: Boolean,
        timeElapsed: Int,
        path: List<CellDto> = emptyList()
    ): WordSubmissionResponse {
        val session = gameSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        if (session.userId != userId) {
            throw IllegalArgumentException("Unauthorized")
        }
        if (!session.isActive) {
            throw IllegalArgumentException("Session is not active")
        }

        val category = categoryRepository.findById(
            session.categoryId ?: throw IllegalArgumentException("Session has no category")
        ).orElseThrow { IllegalArgumentException("Category not found") }

        val targets = wordRepository.findByCategoryId(category.id).map { it.word.uppercase() }.toSet()
        val alreadyFound = userFoundWordRepository.findBySessionId(sessionId).map { it.word.uppercase() }.toSet()

        // Server-authoritative classification when we have a board + traced path; otherwise
        // fall back to legacy word-string matching (target words only, no bonus).
        val board = session.boardState?.let { BoardState.fromJson(it, objectMapper) }
        val cells = path.map { Cell(it.row, it.col) }

        val classification: Classification = if (board != null && cells.size >= 3) {
            wordClassifier.classify(board, cells, targets, alreadyFound)
        } else {
            val w = word.uppercase()
            when {
                alreadyFound.contains(w) -> Classification(WordClass.INVALID, w, "Already found")
                targets.contains(w) -> Classification(WordClass.TARGET, w)
                else -> Classification(WordClass.INVALID, w, "Word not in list")
            }
        }

        if (classification.wordClass == WordClass.INVALID) {
            gameSessionRepository.save(session.copy(currentCombo = 0))
            return WordSubmissionResponse(
                correct = false, score = 0, totalScore = session.totalScore.toLong(),
                currentCombo = 0, combo = 0, levelUp = false, leveledUp = false, newLevel = null,
                wordsFoundInSession = session.wordsFound,
                message = classification.reason.ifBlank { "Not a word" }
            )
        }

        val resolvedWord = classification.word
        val isBonus = classification.wordClass == WordClass.BONUS

        // Diagonal derived from the traced path (server-authoritative); reversed from the
        // client flag (only scales bonus magnitude, low cheat value).
        val diagonal = if (cells.size >= 2) {
            (cells[1].row - cells[0].row) != 0 && (cells[1].col - cells[0].col) != 0
        } else isDiagonal

        val now = LocalDateTime.now()
        val timeSinceLastWord = session.lastWordFoundAt
            ?.let { java.time.Duration.between(it, now).seconds } ?: 0L

        val scoreMode = when {
            session.gameMode == GameMode.CASUAL -> ScoreMode.CASUAL
            isBonus -> ScoreMode.BONUS
            else -> ScoreMode.CLASSIC_TARGET
        }

        // Combo advances only on classic target words; bonus words don't touch the chain.
        val currentCombo = if (scoreMode == ScoreMode.CLASSIC_TARGET) {
            (if (timeSinceLastWord > 30) 0 else session.currentCombo) + 1
        } else {
            session.currentCombo
        }

        val score = scoringService.score(
            ScoreInput(resolvedWord, isReversed, diagonal, timeElapsed, currentCombo, scoreMode)
        )

        userFoundWordRepository.save(
            UserFoundWord(
                userId = userId,
                sessionId = sessionId,
                word = resolvedWord,
                isReversed = isReversed,
                scoreEarned = score,
                isBonus = isBonus,
                wordLength = resolvedWord.length,
                path = if (cells.isNotEmpty()) objectMapper.writeValueAsString(cells) else null
            )
        )

        // Bonus words do NOT count toward puzzle completion.
        val updatedSession = session.copy(
            totalScore = session.totalScore + score,
            wordsFound = if (isBonus) session.wordsFound else session.wordsFound + 1,
            currentCombo = currentCombo,
            highestCombo = maxOf(session.highestCombo, currentCombo),
            lastWordFoundAt = if (scoreMode == ScoreMode.CLASSIC_TARGET) now else session.lastWordFoundAt
        )
        gameSessionRepository.save(updatedSession)

        val userProgress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("User progress not found")

        // Casual play does not affect competitive progression (level/weekly/lifetime score).
        val affectsProgression = session.gameMode != GameMode.CASUAL
        var updatedProgress = userProgress.copy(
            totalScore = if (affectsProgression) userProgress.totalScore + score else userProgress.totalScore,
            weeklyScore = if (affectsProgression) userProgress.weeklyScore + score else userProgress.weeklyScore,
            totalWordsFound = userProgress.totalWordsFound + 1,
            totalReversedWordsFound = if (isReversed) userProgress.totalReversedWordsFound + 1
                                      else userProgress.totalReversedWordsFound,
            totalBonusWordsFound = if (isBonus) userProgress.totalBonusWordsFound + 1
                                   else userProgress.totalBonusWordsFound,
            longestWordFound = maxOf(userProgress.longestWordFound, resolvedWord.length),
            highestCombo = maxOf(userProgress.highestCombo, currentCombo),
            lastPlayedAt = now,
            updatedAt = now
        )

        val newLevel = if (affectsProgression) {
            gameBoardGenerator.calculateLevel(updatedProgress.totalScore)
        } else userProgress.currentLevel
        val leveledUp = newLevel > updatedProgress.currentLevel
        if (leveledUp) {
            updatedProgress = updatedProgress.copy(
                currentLevel = newLevel,
                highestLevelReached = maxOf(updatedProgress.highestLevelReached, newLevel)
            )
        }
        userProgressRepository.save(updatedProgress)

        val breakdown = mapOf(
            "base" to 100 * resolvedWord.length,
            "lengthBonus" to scoringService.lengthBonus(resolvedWord.length),
            "comboMultiplier" to if (scoreMode == ScoreMode.CLASSIC_TARGET)
                scoringService.comboMultiplier(currentCombo) else 1
        )

        return WordSubmissionResponse(
            correct = true,
            score = score,
            totalScore = updatedProgress.totalScore,
            currentCombo = currentCombo,
            combo = currentCombo,
            levelUp = leveledUp,
            leveledUp = leveledUp,
            newLevel = if (leveledUp) newLevel else null,
            wordsFoundInSession = updatedSession.wordsFound,
            message = if (isBonus) "Bonus word!" else "Correct!",
            isBonus = isBonus,
            wordLength = resolvedWord.length,
            coinsEarned = 0, // wired to EconomyService in Phase 2
            coinBalance = updatedProgress.coins,
            scoreBreakdown = breakdown
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

        // Get found words for this session
        val foundWordsInSession = userFoundWordRepository.findBySessionId(session.id)
            .map { it.word }

        return GameSessionResponse(
            sessionId = session.id.toString(),
            level = userProgress.currentLevel,
            gridSize = 10, // Would need to regenerate board or store it
            category = "Unknown", // Would need to track category in session
            grid = emptyList<List<Char>>(),
            words = emptyList(),
            targetWordCount = 0, // Would need to store this
            currentScore = session.totalScore,
            wordsFound = session.wordsFound,
            currentCombo = session.currentCombo,
            gameMode = session.gameMode.name,
            foundWords = foundWordsInSession
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

}

// DTOs specific to service
data class SessionSummary(
    val sessionId: String,
    val startingLevel: Int,
    val endingLevel: Int,
    val totalScore: Int,
    val wordsFound: Int,
    val highestCombo: Int,
    val duration: Long
)
