package com.wordsearch.service

import com.wordsearch.model.*
import com.wordsearch.repository.*
import com.wordsearch.dto.CellDto
import com.wordsearch.dto.GameSessionResponse
import com.wordsearch.dto.HintResponse
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
    private val economyService: EconomyService,
    private val leaderboardService: LeaderboardService,
    private val leagueService: LeagueService,
    private val userRepository: UserRepository,
    private val achievementService: AchievementService,
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

        // Generate game board. Casual mode uses a fixed easy difficulty independent
        // of the player's level; classic scales with level. Words are picked to match
        // the difficulty band and to fit the grid, so harder levels show harder words.
        val config = if (gameMode == GameMode.CASUAL) {
            gameBoardGenerator.getCasualConfig()
        } else {
            gameBoardGenerator.getDifficultyConfig(userProgress.currentLevel)
        }
        val band = if (gameMode == GameMode.CASUAL) 1..2 else difficultyBandForLevel(userProgress.currentLevel)
        val words = selectWordsForBoard(categoryId, config.gridSize, band, config.minWordLength)
        val gameBoard = gameBoardGenerator.generateBoard(
            words = words,
            categoryName = category.name,
            config = config,
            level = userProgress.currentLevel
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

        // Unlock any achievements now satisfied by the updated progress (e.g. First
        // Steps on the very first word, combo/level/word-count milestones). Never let an
        // achievement failure break word submission.
        runCatching { achievementService.checkAndUnlockAchievements(userId) }

        // Project the user's lifetime classic score onto the global leaderboard, and
        // record the points on their current-week league standing so the league screen
        // reflects this round.
        if (affectsProgression) {
            userRepository.findById(userId).orElse(null)?.let { user ->
                // All-time "Highest Ever" board = lifetime total.
                leaderboardService.upsert(
                    userId = userId,
                    username = user.username,
                    boardType = LeaderboardService.BOARD_GLOBAL_CLASSIC,
                    periodKey = LeaderboardService.PERIOD_ALL_TIME,
                    score = updatedProgress.totalScore
                )
                // "This Week" board = points earned this ISO week (resets via the week key).
                leaderboardService.addScore(
                    userId = userId,
                    username = user.username,
                    boardType = LeaderboardService.BOARD_GLOBAL_WEEKLY,
                    periodKey = leaderboardService.currentWeekKey(),
                    delta = score.toLong()
                )
            }
            leagueService.recordWeeklyScore(userId, score.toLong())
        }

        // Bonus words award coins; classic targets do not. coinBalance always
        // reflects the user's wallet after this submission.
        val coinsEarned: Long
        val coinBalance: Long
        if (isBonus) {
            coinsEarned = economyService.bonusWordCoins(resolvedWord.length)
            coinBalance = economyService.earn(userId, coinsEarned, "BONUS_WORD", null)
        } else {
            coinsEarned = 0
            coinBalance = updatedProgress.coins
        }

        // Full, honest breakdown of every term that contributed to `score`.
        val core = 100 * resolvedWord.length + scoringService.lengthBonus(resolvedWord.length)
        val isClassic = scoreMode == ScoreMode.CLASSIC_TARGET
        val orientationApplies = scoreMode != ScoreMode.CASUAL
        val breakdown = mapOf(
            "base" to 100 * resolvedWord.length,
            "lengthBonus" to scoringService.lengthBonus(resolvedWord.length),
            "reverseBonus" to if (orientationApplies && isReversed) (core * 0.5).toInt() else 0,
            "diagonalBonus" to if (orientationApplies && diagonal) (core * 0.25).toInt() else 0,
            "speed" to if (isClassic) minOf(100, maxOf(0, (60 - timeElapsed) * 2)) else 0,
            "comboMultiplier" to if (isClassic) scoringService.comboMultiplier(currentCombo) else 1
        )

        return WordSubmissionResponse(
            correct = true,
            score = score,
            totalScore = updatedProgress.totalScore,
            sessionScore = updatedSession.totalScore.toInt(),
            currentCombo = currentCombo,
            combo = currentCombo,
            levelUp = leveledUp,
            leveledUp = leveledUp,
            newLevel = if (leveledUp) newLevel else null,
            wordsFoundInSession = updatedSession.wordsFound,
            message = if (isBonus) "Bonus word!" else "Correct!",
            isBonus = isBonus,
            wordLength = resolvedWord.length,
            coinsEarned = coinsEarned,
            coinBalance = coinBalance,
            scoreBreakdown = breakdown
        )
    }

    /**
     * Reveals the first not-yet-found target word for [sessionId], charging
     * [EconomyService.HINT_COST] coins. The board is parsed server-side so the
     * revealed cell path is authoritative. Throws if the session is missing/inactive,
     * not owned by [userId], has no board, or has no unfound target words left.
     * Propagates [InsufficientCoinsException] when the player cannot afford it.
     */
    @Transactional
    fun useHint(sessionId: UUID, userId: UUID): HintResponse {
        val session = gameSessionRepository.findById(sessionId)
            .orElseThrow { IllegalArgumentException("Session not found") }

        if (session.userId != userId) {
            throw IllegalArgumentException("Unauthorized")
        }
        if (!session.isActive) {
            throw IllegalArgumentException("Session is not active")
        }

        val board = session.boardState?.let { BoardState.fromJson(it, objectMapper) }
            ?: throw IllegalArgumentException("Session has no board")

        val foundTargets = userFoundWordRepository.findBySessionId(sessionId)
            .filter { !it.isBonus }
            .map { it.word.uppercase() }
            .toSet()

        val reveal = board.solution.firstOrNull { it.word.uppercase() !in foundTargets }
            ?: throw IllegalArgumentException("No words left to reveal")

        val coinBalance = economyService.spend(userId, EconomyService.HINT_COST, "HINT", sessionId)

        return HintResponse(
            word = reveal.word,
            cells = reveal.path.map { CellDto(it.row, it.col) },
            coinsSpent = EconomyService.HINT_COST,
            coinBalance = coinBalance
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
        var casualBest = 0L
        var casualGames = 0
        var casualWeekly = 0L
        if (session.gameMode == GameMode.CASUAL) {
            val userProgress = userProgressRepository.findByUserId(userId)
                ?: throw IllegalArgumentException("User progress not found")

            val sessionScore = session.totalScore.toLong()
            val week = leagueService.weekKey(java.time.LocalDate.now())
            // Cumulative casual score for the current week; resets when the week changes.
            val weeklyBefore = if (userProgress.casualWeekKey == week) userProgress.casualWeeklyScore else 0L

            val updatedProgress = userProgress.copy(
                casualPuzzlesCompleted = userProgress.casualPuzzlesCompleted + 1,
                casualBestScore = maxOf(userProgress.casualBestScore, sessionScore),
                casualWeeklyScore = weeklyBefore + sessionScore,
                casualWeekKey = week,
                lastPlayedAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            userProgressRepository.save(updatedProgress)
            casualBest = updatedProgress.casualBestScore
            casualGames = updatedProgress.casualPuzzlesCompleted
            casualWeekly = updatedProgress.casualWeeklyScore

            // Project both the all-time best and this week's cumulative onto the casual boards.
            userRepository.findById(userId).orElse(null)?.let { user ->
                leaderboardService.upsert(
                    userId, user.username,
                    LeaderboardService.BOARD_CASUAL_BEST, LeaderboardService.PERIOD_ALL_TIME,
                    updatedProgress.casualBestScore
                )
                leaderboardService.upsert(
                    userId, user.username,
                    LeaderboardService.BOARD_CASUAL_WEEKLY, week,
                    updatedProgress.casualWeeklyScore
                )
            }
        }

        return SessionSummary(
            sessionId = sessionId.toString(),
            startingLevel = session.startingLevel,
            endingLevel = updatedSession.endingLevel ?: session.startingLevel,
            totalScore = session.totalScore,
            wordsFound = session.wordsFound,
            highestCombo = session.highestCombo,
            duration = java.time.Duration.between(session.sessionStart, updatedSession.sessionEnd).toMinutes(),
            casualBestScore = casualBest,
            casualGamesPlayed = casualGames,
            casualWeeklyScore = casualWeekly
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

    /**
     * Difficulty (1-5) band of words to draw for a given player level, across the
     * 0–1000 competitive scale: easy (1–100), medium (101–400), hard (401–1000).
     */
    private fun difficultyBandForLevel(level: Int): IntRange = when {
        level <= 25 -> 1..2
        level <= 60 -> 1..3
        level <= 100 -> 2..3
        level <= 200 -> 2..4
        level <= 400 -> 3..4
        level <= 600 -> 3..5
        else -> 4..5
    }

    /**
     * Pick the category's words that match the difficulty [band] and fit the grid,
     * shuffled. Honors [minWordLength] so higher tiers show longer words, but widens
     * both the band and length floor if a category lacks enough words so a board is
     * always produced.
     */
    private fun selectWordsForBoard(
        categoryId: UUID,
        gridSize: Int,
        band: IntRange,
        minWordLength: Int = 3
    ): List<String> {
        val all = wordRepository.findByCategoryId(categoryId)
        if (all.isEmpty()) return emptyList()
        val fits = all.filter { it.word.length in minWordLength..gridSize }
        // Fall back to the absolute 3..gridSize window if the length floor is too strict.
        val candidates = when {
            fits.isNotEmpty() -> fits
            else -> all.filter { it.word.length in 3..gridSize }.ifEmpty { all }
        }
        var pool = candidates.filter { it.difficultyLevel in band }
        if (pool.size < 8) pool = candidates.filter { it.difficultyLevel <= band.last }
        if (pool.size < 8) pool = candidates
        return pool.shuffled().map { it.word }
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
    val duration: Long,
    // Casual stats (0 for classic sessions)
    val casualBestScore: Long = 0,
    val casualGamesPlayed: Int = 0,
    val casualWeeklyScore: Long = 0
)
