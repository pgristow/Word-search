package com.wordsearch.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wordsearch.model.*
import com.wordsearch.repository.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

/**
 * Casual-mode progression: ending a casual session records casual_best_score,
 * but only ever raises it (a worse run does not lower the recorded best).
 */
class GameSessionServiceCasualTest {
    private val mapper = jacksonObjectMapper()
    private lateinit var gameSessionRepository: GameSessionRepository
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var wordRepository: WordRepository
    private lateinit var userFoundWordRepository: UserFoundWordRepository
    private lateinit var economyService: EconomyService
    private lateinit var service: GameSessionService

    private val userId = UUID.randomUUID()
    private val sessionId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        gameSessionRepository = mockk(relaxed = true)
        userProgressRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        wordRepository = mockk(relaxed = true)
        userFoundWordRepository = mockk(relaxed = true)
        economyService = mockk(relaxed = true)
        val dictionary = DictionaryService("/data/words_test.txt", 3)
        service = GameSessionService(
            gameSessionRepository, userProgressRepository,
            GameBoardGenerator(wordRepository, categoryRepository),
            categoryRepository, wordRepository, userFoundWordRepository,
            ScoringService(), WordClassifier(dictionary), economyService, mapper
        )
        every { gameSessionRepository.save(any()) } answers { firstArg() }
        every { userProgressRepository.save(any()) } answers { firstArg() }
    }

    private fun casualSession(score: Int) = GameSession(
        id = sessionId, userId = userId,
        startingLevel = 1, gameMode = GameMode.CASUAL, isActive = true,
        totalScore = score
    )

    @Test fun `casual endSession raises casual best score when run is better`() {
        every { gameSessionRepository.findById(sessionId) } returns Optional.of(casualSession(900))
        every { userProgressRepository.findByUserId(userId) } returns
            UserProgress(userId = userId, casualBestScore = 500L)

        val saved = slot<UserProgress>()
        every { userProgressRepository.save(capture(saved)) } answers { firstArg() }

        service.endSession(sessionId, userId)

        assertEquals(900L, saved.captured.casualBestScore)
    }

    @Test fun `casual endSession does not lower casual best score on a worse run`() {
        every { gameSessionRepository.findById(sessionId) } returns Optional.of(casualSession(300))
        every { userProgressRepository.findByUserId(userId) } returns
            UserProgress(userId = userId, casualBestScore = 800L)

        val saved = slot<UserProgress>()
        every { userProgressRepository.save(capture(saved)) } answers { firstArg() }

        service.endSession(sessionId, userId)

        assertEquals(800L, saved.captured.casualBestScore)
    }
}
