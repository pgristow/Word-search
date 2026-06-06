package com.wordsearch.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.wordsearch.dto.CellDto
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
 * Verifies the rewritten, server-authoritative submitWord:
 *  - target words on their path are accepted,
 *  - real off-list words are recognized as bonus,
 *  - you cannot claim a word you did not actually trace (anti-spoof).
 */
class GameSessionServiceSubmitTest {
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
        solution = listOf(SolutionWord("CAT", listOf(Cell(0, 0), Cell(0, 1), Cell(0, 2)), false, "HORIZONTAL"))
    )

    @BeforeEach
    fun setup() {
        gameSessionRepository = mockk(relaxed = true)
        userProgressRepository = mockk(relaxed = true)
        categoryRepository = mockk()
        wordRepository = mockk()
        userFoundWordRepository = mockk(relaxed = true)
        val dictionary = DictionaryService("/data/words_test.txt", 3)
        economyService = mockk(relaxed = true)
        every { economyService.bonusWordCoins(any()) } answers { 5L + 2L * maxOf(0, firstArg<Int>() - 3) }
        every { economyService.earn(any(), any(), any(), any()) } returns 42L
        leaderboardService = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        every { userRepository.findById(any()) } returns Optional.of(
            User(id = userId, username = "tester", email = "t@e.com", passwordHash = "x")
        )
        service = GameSessionService(
            gameSessionRepository, userProgressRepository,
            GameBoardGenerator(wordRepository, categoryRepository),
            categoryRepository, wordRepository, userFoundWordRepository,
            ScoringService(), WordClassifier(dictionary), economyService,
            leaderboardService, mockk(relaxed = true), userRepository, mapper
        )

        val session = GameSession(
            id = sessionId, userId = userId, categoryId = categoryId,
            startingLevel = 1, gameMode = GameMode.CLASSIC, isActive = true,
            boardState = mapper.writeValueAsString(board)
        )
        every { gameSessionRepository.findById(sessionId) } returns Optional.of(session)
        every { categoryRepository.findById(categoryId) } returns Optional.of(Category(id = categoryId, name = "Animals"))
        every { wordRepository.findByCategoryId(categoryId) } returns listOf(
            Word(id = UUID.randomUUID(), word = "CAT", categoryId = categoryId)
        )
        every { userFoundWordRepository.findBySessionId(sessionId) } returns emptyList()
        every { userProgressRepository.findByUserId(userId) } returns UserProgress(userId = userId)
        // Generic save(S):S returns Object under relaxed mockk; echo the argument back.
        every { gameSessionRepository.save(any()) } answers { firstArg() }
        every { userFoundWordRepository.save(any()) } answers { firstArg() }
        every { userProgressRepository.save(any()) } answers { firstArg() }
    }

    private fun path(vararg cells: Pair<Int, Int>) = cells.map { CellDto(it.first, it.second) }

    @Test fun `target word on its path is accepted`() {
        val r = service.submitWord(sessionId, userId, "CAT", false, false, 999, path(0 to 0, 0 to 1, 0 to 2))
        assertTrue(r.correct)
        assertFalse(r.isBonus)
        assertEquals(1, r.wordsFoundInSession)
        assertEquals(3, r.wordLength)
    }

    @Test fun `real off-list word is recognized as bonus and does not count toward target`() {
        val r = service.submitWord(sessionId, userId, "DOG", false, false, 999, path(2 to 0, 2 to 1, 2 to 2))
        assertTrue(r.correct)
        assertTrue(r.isBonus)
        assertEquals(0, r.wordsFoundInSession) // bonus does not advance puzzle completion
        assertEquals("Bonus word!", r.message)
        assertEquals(5L, r.coinsEarned) // DOG len 3 -> bonusWordCoins(3) = 5
        assertEquals(42L, r.coinBalance) // balance returned by economyService.earn stub
    }

    @Test fun `cannot claim a target word without tracing it (anti-spoof)`() {
        // Claim CAT but trace row 1 (XYZ, gibberish) -> rejected
        val r = service.submitWord(sessionId, userId, "CAT", false, false, 999, path(1 to 0, 1 to 1, 1 to 2))
        assertFalse(r.correct)
    }

    @Test fun `longer word scores more than base via length bonus`() {
        // CAT (len 3) classic combo1, t=999: base 300, no length bonus, mult 1 => 300
        val r = service.submitWord(sessionId, userId, "CAT", false, false, 999, path(0 to 0, 0 to 1, 0 to 2))
        assertEquals(300, r.score)
    }
}
