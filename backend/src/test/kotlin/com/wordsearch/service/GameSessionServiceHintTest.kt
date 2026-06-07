package com.wordsearch.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wordsearch.model.*
import com.wordsearch.repository.*
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

/**
 * Verifies useHint:
 *  - reveals the first not-yet-found target word and spends HINT_COST,
 *  - propagates InsufficientCoinsException untouched,
 *  - throws when every target word has already been found.
 */
class GameSessionServiceHintTest {
    private val mapper = jacksonObjectMapper()
    private lateinit var gameSessionRepository: GameSessionRepository
    private lateinit var userProgressRepository: UserProgressRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var wordRepository: WordRepository
    private lateinit var userFoundWordRepository: UserFoundWordRepository
    private lateinit var economyService: EconomyService
    private lateinit var leaderboardService: LeaderboardService
    private lateinit var userRepository: UserRepository
    private lateinit var service: GameSessionService

    private val userId = UUID.randomUUID()
    private val sessionId = UUID.randomUUID()
    private val categoryId = UUID.randomUUID()

    private val board = BoardState(
        gridSize = 3,
        grid = listOf("CAT", "XYZ", "DOG"),
        solution = listOf(
            SolutionWord("CAT", listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)), false, "HORIZONTAL"),
            SolutionWord("DOG", listOf(Cell(2, 0), Cell(2, 1), Cell(2, 2)), false, "HORIZONTAL")
        )
    )

    @BeforeEach
    fun setup() {
        gameSessionRepository = mockk(relaxed = true)
        userProgressRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        wordRepository = mockk(relaxed = true)
        userFoundWordRepository = mockk(relaxed = true)
        economyService = mockk(relaxed = true)
        leaderboardService = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        val dictionary = DictionaryService("/data/words_test.txt", 3)
        service = GameSessionService(
            gameSessionRepository, userProgressRepository,
            GameBoardGenerator(wordRepository, categoryRepository),
            categoryRepository, wordRepository, userFoundWordRepository,
            ScoringService(), WordClassifier(dictionary), economyService,
            leaderboardService, mockk(relaxed = true), userRepository, mockk(relaxed = true), mapper
        )

        val session = GameSession(
            id = sessionId, userId = userId, categoryId = categoryId,
            startingLevel = 1, gameMode = GameMode.CLASSIC, isActive = true,
            boardState = mapper.writeValueAsString(board)
        )
        every { gameSessionRepository.findById(sessionId) } returns Optional.of(session)
    }

    @Test fun `reveals first unfound target and spends hint cost`() {
        // CAT already found as a target; DOG is next.
        every { userFoundWordRepository.findBySessionId(sessionId) } returns listOf(
            UserFoundWord(userId = userId, sessionId = sessionId, word = "cat", scoreEarned = 0, isBonus = false)
        )
        every { economyService.spend(userId, 30L, "HINT", sessionId) } returns 70L

        val r = service.useHint(sessionId, userId)

        assertEquals("DOG", r.word)
        assertEquals(listOf(2 to 0, 2 to 1, 2 to 2), r.cells.map { it.row to it.col })
        assertEquals(30L, r.coinsSpent)
        assertEquals(70L, r.coinBalance)
        verify { economyService.spend(userId, 30L, "HINT", sessionId) }
    }

    @Test fun `ignores bonus words when computing found targets`() {
        // A found bonus word must not consume a hint slot; CAT is still the first target.
        every { userFoundWordRepository.findBySessionId(sessionId) } returns listOf(
            UserFoundWord(userId = userId, sessionId = sessionId, word = "AXY", scoreEarned = 0, isBonus = true)
        )
        every { economyService.spend(userId, 30L, "HINT", sessionId) } returns 70L

        val r = service.useHint(sessionId, userId)

        assertEquals("CAT", r.word)
    }

    @Test fun `insufficient coins propagates and reveals nothing`() {
        every { userFoundWordRepository.findBySessionId(sessionId) } returns emptyList()
        every { economyService.spend(userId, 30L, "HINT", sessionId) } throws
            InsufficientCoinsException("Insufficient coins")

        assertThrows(InsufficientCoinsException::class.java) {
            service.useHint(sessionId, userId)
        }
    }

    @Test fun `throws when no target words are left to reveal`() {
        every { userFoundWordRepository.findBySessionId(sessionId) } returns listOf(
            UserFoundWord(userId = userId, sessionId = sessionId, word = "CAT", scoreEarned = 0, isBonus = false),
            UserFoundWord(userId = userId, sessionId = sessionId, word = "DOG", scoreEarned = 0, isBonus = false)
        )

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.useHint(sessionId, userId)
        }
        assertEquals("No words left to reveal", ex.message)
        verify(exactly = 0) { economyService.spend(any(), any(), any(), any()) }
    }
}
