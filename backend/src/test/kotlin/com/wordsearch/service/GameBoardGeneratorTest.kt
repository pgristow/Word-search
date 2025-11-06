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

        assertEquals(10, gameBoard.gridSize, "Level 1 should have 10x10 grid")
        assertEquals(10, gameBoard.grid.size, "Grid should have 10 rows")
        assertEquals(10, gameBoard.grid[0].size, "Grid should have 10 columns")
    }

    @Test
    fun `test generateBoard creates correct grid size for level 15`() {
        val words = listOf("CAT", "DOG", "BIRD", "FISH", "LION", "BEAR", "WOLF", "DEER")
        val gameBoard = gameBoardGenerator.generateBoard(15, words, "Animals")

        assertEquals(12, gameBoard.gridSize, "Level 15 should have 12x12 grid")
        assertEquals(12, gameBoard.grid.size, "Grid should have 12 rows")
    }

    @Test
    fun `test generateBoard creates correct grid size for level 30`() {
        val words = listOf("CAT", "DOG", "BIRD", "FISH", "LION", "BEAR", "WOLF", "DEER")
        val gameBoard = gameBoardGenerator.generateBoard(30, words, "Animals")

        assertEquals(15, gameBoard.gridSize, "Level 30 should have 15x15 grid")
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

        assertEquals(10, config.gridSize)
        assertEquals(0f, config.reverseWordProbability)
        assertEquals(3, config.minWordLength)
        assertEquals(8, config.targetWordCount)
    }

    @Test
    fun `test getDifficultyConfig returns correct config for level 11-15`() {
        val config = gameBoardGenerator.getDifficultyConfig(12)

        assertEquals(12, config.gridSize)
        assertEquals(0.3f, config.reverseWordProbability)
        assertEquals(4, config.minWordLength)
        assertEquals(12, config.targetWordCount)
    }

    @Test
    fun `test getDifficultyConfig returns correct config for level 41+`() {
        val config = gameBoardGenerator.getDifficultyConfig(50)

        assertEquals(20, config.gridSize)
        assertEquals(0.7f, config.reverseWordProbability)
        assertEquals(5, config.minWordLength)
        assertEquals(20, config.targetWordCount)
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

        // baseScore = 100 * 3 = 300
        // speedBonus = (60 - 30) * 10 = 300
        // total = 600 * 1 = 600
        assertEquals(600, score)
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

        // baseScore = 100 * 8 = 800
        // reverseBonus = 800 * 0.5 = 400
        // speedBonus = (60 - 20) * 10 = 400
        // total = (800 + 400 + 400) * 1 = 1600
        assertEquals(1600, score)
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

        // baseScore = 100 * 4 = 400
        // diagonalBonus = 400 * 0.25 = 100
        // speedBonus = (60 - 15) * 10 = 450
        // total = (400 + 100 + 450) * 1 = 950
        assertEquals(950, score)
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

        // baseScore = 100 * 3 = 300
        // speedBonus = (60 - 10) * 10 = 500
        // total = (300 + 500) * 3 = 2400 (combo 5-9 = 3x multiplier)
        assertEquals(2400, score)
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

        // baseScore = 100 * 3 = 300
        // speedBonus = (60 - 5) * 10 = 550
        // total = (300 + 550) * 4 = 3400 (combo 10+ = 4x multiplier)
        assertEquals(3400, score)
    }

    @Test
    fun `test calculateLevel from score`() {
        assertEquals(1, gameBoardGenerator.calculateLevel(0))
        assertEquals(1, gameBoardGenerator.calculateLevel(500))
        assertEquals(2, gameBoardGenerator.calculateLevel(1500))
        assertEquals(5, gameBoardGenerator.calculateLevel(7500))
        assertEquals(10, gameBoardGenerator.calculateLevel(27500))
    }

    @Test
    fun `test generateBoardForCategory creates board with correct category`() {
        val categoryId = UUID.randomUUID()
        val category = Category(
            id = categoryId,
            name = "Animals",
            unlockLevel = 1
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
            unlockLevel = 1
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
}
