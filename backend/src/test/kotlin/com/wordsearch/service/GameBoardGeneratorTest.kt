package com.wordsearch.service

import com.wordsearch.model.Category
import com.wordsearch.model.Word
import com.wordsearch.repository.CategoryRepository
import com.wordsearch.repository.WordRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional
import java.util.UUID

class GameBoardGeneratorTest {

    private lateinit var wordRepository: WordRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var gameBoardGenerator: GameBoardGenerator

    @BeforeEach
    fun setup() {
        wordRepository = mockk()
        categoryRepository = mockk()
        gameBoardGenerator = GameBoardGenerator(wordRepository, categoryRepository)
    }

    @Test
    fun `test generateBoard creates correct grid size for level 1`() {
        val words = listOf("CAT", "DOG", "BIRD", "FISH", "LION", "BEAR", "WOLF", "DEER")
        val gameBoard = gameBoardGenerator.generateBoard(1, words, "Animals")

        assertEquals(7, gameBoard.gridSize, "Level 1 should have 7x7 grid")
        assertEquals(7, gameBoard.grid.size, "Grid should have 7 rows")
        assertEquals(7, gameBoard.grid[0].size, "Grid should have 7 columns")
    }

    @Test
    fun `test generateBoard creates correct grid size for level 15`() {
        val words = listOf("CAT", "DOG", "BIRD", "FISH", "LION", "BEAR", "WOLF", "DEER")
        val gameBoard = gameBoardGenerator.generateBoard(15, words, "Animals")

        assertEquals(8, gameBoard.gridSize, "Level 15 should have 8x8 grid")
        assertEquals(8, gameBoard.grid.size, "Grid should have 8 rows")
    }

    @Test
    fun `test generateBoard creates correct grid size for level 30`() {
        val words = listOf("CAT", "DOG", "BIRD", "FISH", "LION", "BEAR", "WOLF", "DEER")
        val gameBoard = gameBoardGenerator.generateBoard(30, words, "Animals")

        assertEquals(8, gameBoard.gridSize, "Level 30 should have 8x8 grid")
    }

    @Test
    fun `test generateBoard places words on the grid`() {
        val words = listOf("CAT", "DOG", "BIRD")
        val gameBoard = gameBoardGenerator.generateBoard(1, words, "Animals")

        assertTrue(gameBoard.placedWords.isNotEmpty(), "Should have placed words on the board")
        assertTrue(gameBoard.placedWords.size <= words.size, "Should not place more words than provided")
    }

    @Test
    fun `test getDifficultyConfig returns correct config for level 1-5`() {
        val config = gameBoardGenerator.getDifficultyConfig(3)

        assertEquals(7, config.gridSize)
        assertEquals(0f, config.reverseWordProbability)
        assertEquals(3, config.minWordLength)
        assertEquals(5, config.targetWordCount)
    }

    @Test
    fun `test getDifficultyConfig returns correct config for level 11-15`() {
        val config = gameBoardGenerator.getDifficultyConfig(12)

        assertEquals(8, config.gridSize)
        assertEquals(0.3f, config.reverseWordProbability)
        assertEquals(4, config.minWordLength)
        assertEquals(7, config.targetWordCount)
    }

    @Test
    fun `test getDifficultyConfig returns correct config for level 41+`() {
        val config = gameBoardGenerator.getDifficultyConfig(50)

        assertEquals(8, config.gridSize)
        assertEquals(0.8f, config.reverseWordProbability)
        assertEquals(5, config.minWordLength)
        assertEquals(8, config.targetWordCount)
    }

    @Test
    fun `test calculateWordScore with basic word`() {
        val score = gameBoardGenerator.calculateWordScore(
            word = "CAT",
            isReversed = false,
            isDiagonal = false,
            timeElapsed = 30,
            currentCombo = 0
        )

        // baseScore = 100 * 3 = 300; speedBonus = min(100,(60-30)*2)=60; total=(300+60)*1=360


        assertEquals(360, score)
    }

    @Test
    fun `test calculateWordScore with reversed word`() {
        val score = gameBoardGenerator.calculateWordScore(
            word = "ELEPHANT",
            isReversed = true,
            isDiagonal = false,
            timeElapsed = 20,
            currentCombo = 0
        )

        // base 800; reverse 400; speedBonus = min(100,(60-20)*2)=80; total=(800+400+80)*1=1280



        assertEquals(1280, score)
    }

    @Test
    fun `test calculateWordScore with diagonal word`() {
        val score = gameBoardGenerator.calculateWordScore(
            word = "BIRD",
            isReversed = false,
            isDiagonal = true,
            timeElapsed = 15,
            currentCombo = 0
        )

        // base 400; diagonal 100; speedBonus = min(100,(60-15)*2)=90; total=(400+100+90)*1=590



        assertEquals(590, score)
    }

    @Test
    fun `test calculateWordScore with combo multiplier`() {
        val score = gameBoardGenerator.calculateWordScore(
            word = "DOG",
            isReversed = false,
            isDiagonal = false,
            timeElapsed = 10,
            currentCombo = 5
        )

        // base 300; speedBonus = min(100,(60-10)*2)=100; total=(300+100)*3=1200 (combo 5-9 -> 3x)


        assertEquals(1200, score)
    }

    @Test
    fun `test calculateWordScore with maximum combo`() {
        val score = gameBoardGenerator.calculateWordScore(
            word = "CAT",
            isReversed = false,
            isDiagonal = false,
            timeElapsed = 5,
            currentCombo = 15
        )

        // base 300; speedBonus = min(100,(60-5)*2 capped)=100; total=(300+100)*4=1600 (combo 10+ -> 4x)


        assertEquals(1600, score)
    }

    @Test
    fun `test calculateLevel from score`() {
        assertEquals(1, gameBoardGenerator.calculateLevel(0))
        assertEquals(1, gameBoardGenerator.calculateLevel(500))
        assertEquals(1, gameBoardGenerator.calculateLevel(1500))
        assertEquals(3, gameBoardGenerator.calculateLevel(7500))
        assertEquals(6, gameBoardGenerator.calculateLevel(27500))
    }

    @Test
    fun `test generateBoardForCategory creates board with correct category`() {
        val categoryId = UUID.randomUUID()
        val category = Category(
            id = categoryId,
            name = "Animals",
        )
        val words = listOf(
            Word(id = UUID.randomUUID(), word = "CAT", categoryId = categoryId),
            Word(id = UUID.randomUUID(), word = "DOG", categoryId = categoryId),
            Word(id = UUID.randomUUID(), word = "BIRD", categoryId = categoryId)
        )

        every { categoryRepository.findById(categoryId) } returns Optional.of(category)
        every { wordRepository.findByCategoryId(categoryId) } returns words

        val gameBoard = gameBoardGenerator.generateBoardForCategory(
            categoryId = categoryId,
            difficultyLevel = 1,
            targetWordCount = 3
        )

        assertEquals("Animals", gameBoard.category)
        assertTrue(gameBoard.placedWords.size <= 3)
    }

    @Test
    fun `test generateBoardForCategory throws exception when category not found`() {
        val categoryId = UUID.randomUUID()

        every { categoryRepository.findById(categoryId) } returns Optional.empty()

        assertThrows(IllegalArgumentException::class.java) {
            gameBoardGenerator.generateBoardForCategory(
                categoryId = categoryId,
                difficultyLevel = 1,
                targetWordCount = 5
            )
        }
    }

    @Test
    fun `test generateBoardForCategory throws exception when no words found`() {
        val categoryId = UUID.randomUUID()
        val category = Category(
            id = categoryId,
            name = "Empty",
        )

        every { categoryRepository.findById(categoryId) } returns Optional.of(category)
        every { wordRepository.findByCategoryId(categoryId) } returns emptyList()

        assertThrows(IllegalStateException::class.java) {
            gameBoardGenerator.generateBoardForCategory(
                categoryId = categoryId,
                difficultyLevel = 1,
                targetWordCount = 5
            )
        }
    }

    @Test
    fun `test grid is fully filled with letters`() {
        val words = listOf("CAT", "DOG")
        val gameBoard = gameBoardGenerator.generateBoard(1, words, "Animals")

        // Check that no cell is empty (space character)
        for (row in gameBoard.grid) {
            for (cell in row) {
                assertNotEquals(' ', cell, "Grid should not have empty cells")
            }
        }
    }

    @Test
    fun `getCasualConfig returns a fixed easy config independent of level`() {
        val config = gameBoardGenerator.getCasualConfig()

        assertEquals(8, config.gridSize)
        assertEquals(
            listOf(Direction.HORIZONTAL, Direction.VERTICAL),
            config.allowedDirections
        )
        assertEquals(0f, config.reverseWordProbability)
        assertEquals(3, config.minWordLength)
        assertEquals(8, config.targetWordCount)
        assertEquals("ETAOINSHRDLUCMFWYPVBGKJQXZ", config.distractorLetters)
        // Identical on repeated calls (no level/state dependence).
        assertEquals(config, gameBoardGenerator.getCasualConfig())
    }
}
