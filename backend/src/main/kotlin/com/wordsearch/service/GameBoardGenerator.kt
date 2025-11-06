package com.wordsearch.service

import org.springframework.stereotype.Service
import kotlin.math.sqrt
import kotlin.random.Random

@Service
class GameBoardGenerator {

    /**
     * Generates an endless word search board that scales with level
     */
    fun generateBoard(
        level: Int,
        words: List<String>,
        categoryName: String
    ): GameBoard {
        val config = getDifficultyConfig(level)
        val grid = Array(config.gridSize) { CharArray(config.gridSize) { ' ' } }
        val placedWords = mutableListOf<PlacedWord>()

        // Sort words by length (longest first for better placement)
        val sortedWords = words.take(config.targetWordCount).sortedByDescending { it.length }

        for (word in sortedWords) {
            val shouldReverse = Random.nextFloat() < config.reverseWordProbability
            val wordToPlace = if (shouldReverse) word.reversed() else word

            val placement = findPlacement(grid, wordToPlace, config)
            if (placement != null) {
                placeWord(grid, wordToPlace, placement)
                placedWords.add(
                    PlacedWord(
                        word = word,
                        displayWord = wordToPlace,
                        isReversed = shouldReverse,
                        startRow = placement.row,
                        startCol = placement.col,
                        direction = placement.direction
                    )
                )
            }
        }

        // Fill empty cells with random letters
        fillEmptyCells(grid, config.distractorLetters)

        return GameBoard(
            grid = grid,
            placedWords = placedWords,
            level = level,
            gridSize = config.gridSize,
            category = categoryName
        )
    }

    /**
     * Gets difficulty configuration based on current level
     */
    private fun getDifficultyConfig(level: Int): DifficultyConfig {
        return when {
            level in 1..5 -> DifficultyConfig(
                gridSize = 10,
                allowedDirections = listOf(Direction.HORIZONTAL, Direction.VERTICAL),
                reverseWordProbability = 0f,
                minWordLength = 3,
                targetWordCount = 8,
                distractorLetters = "ETAOINSHRDLU"
            )
            level in 6..10 -> DifficultyConfig(
                gridSize = 12,
                allowedDirections = listOf(
                    Direction.HORIZONTAL,
                    Direction.VERTICAL,
                    Direction.DIAGONAL_DOWN_RIGHT
                ),
                reverseWordProbability = 0f,
                minWordLength = 4,
                targetWordCount = 10,
                distractorLetters = "ETAOINSHRDLUCMFWYPVBGKJQXZ"
            )
            level in 11..15 -> DifficultyConfig(
                gridSize = 12,
                allowedDirections = Direction.values().toList(),
                reverseWordProbability = 0.3f,
                minWordLength = 4,
                targetWordCount = 10,
                distractorLetters = "ETAOINSHRDLUCMFWYPVBGKJQXZ"
            )
            level in 16..20 -> DifficultyConfig(
                gridSize = 15,
                allowedDirections = Direction.values().toList(),
                reverseWordProbability = 0.5f,
                minWordLength = 5,
                targetWordCount = 12,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
            level in 21..30 -> DifficultyConfig(
                gridSize = 15,
                allowedDirections = Direction.values().toList(),
                reverseWordProbability = 0.6f,
                minWordLength = 5,
                targetWordCount = 12,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
            level in 31..40 -> DifficultyConfig(
                gridSize = 18,
                allowedDirections = Direction.values().toList(),
                reverseWordProbability = 0.7f,
                minWordLength = 6,
                targetWordCount = 15,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
            else -> DifficultyConfig(
                gridSize = 20,
                allowedDirections = Direction.values().toList(),
                reverseWordProbability = 0.8f,
                minWordLength = 6,
                targetWordCount = 15,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
        }
    }

    /**
     * Finds valid placement for a word
     */
    private fun findPlacement(
        grid: Array<CharArray>,
        word: String,
        config: DifficultyConfig
    ): Placement? {
        val maxAttempts = 100

        repeat(maxAttempts) {
            val row = Random.nextInt(grid.size)
            val col = Random.nextInt(grid.size)
            val direction = config.allowedDirections.random()

            if (canPlaceWord(grid, word, row, col, direction)) {
                return Placement(row, col, direction)
            }
        }

        return null
    }

    /**
     * Checks if a word can be placed at the given position
     */
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

            // Out of bounds check
            if (r !in grid.indices || c !in grid[r].indices) {
                return false
            }

            // Cell must be empty or contain the same letter
            if (grid[r][c] != ' ' && grid[r][c] != word[i]) {
                return false
            }
        }

        return true
    }

    /**
     * Places a word on the grid
     */
    private fun placeWord(
        grid: Array<CharArray>,
        word: String,
        placement: Placement
    ) {
        val (dr, dc) = placement.direction.deltas

        for (i in word.indices) {
            val r = placement.row + dr * i
            val c = placement.col + dc * i
            grid[r][c] = word[i]
        }
    }

    /**
     * Fills empty cells with random letters
     */
    private fun fillEmptyCells(grid: Array<CharArray>, letters: String) {
        for (r in grid.indices) {
            for (c in grid[r].indices) {
                if (grid[r][c] == ' ') {
                    grid[r][c] = letters.random()
                }
            }
        }
    }

    /**
     * Calculates user's current level based on score
     */
    fun calculateLevel(totalScore: Long): Int {
        if (totalScore == 0L) return 1

        // Level N requires sum of 1..N * 1000 = N*(N+1)*500
        // Solve: totalScore = N*(N+1)*500
        val a = 1.0
        val b = 1.0
        val c = -(totalScore / 500.0)
        val level = ((-b + sqrt(b * b - 4 * a * c)) / (2 * a)).toInt()
        return maxOf(1, level)
    }

    /**
     * Calculates score for finding a word
     */
    fun calculateWordScore(
        word: String,
        isReversed: Boolean,
        isDiagonal: Boolean,
        timeElapsed: Int,
        currentCombo: Int
    ): Int {
        val baseScore = 100 * word.length
        val reverseBonus = if (isReversed) (baseScore * 0.5).toInt() else 0
        val diagonalBonus = if (isDiagonal) (baseScore * 0.25).toInt() else 0
        val speedBonus = maxOf(0, (60 - timeElapsed) * 10)
        val comboMultiplier = when (currentCombo) {
            in 2..4 -> 2
            in 5..9 -> 3
            in 10..Int.MAX_VALUE -> 4
            else -> 1
        }

        return (baseScore + reverseBonus + diagonalBonus + speedBonus) * comboMultiplier
    }
}

// Data classes
data class GameBoard(
    val grid: Array<CharArray>,
    val placedWords: List<PlacedWord>,
    val level: Int,
    val gridSize: Int,
    val category: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameBoard

        if (!grid.contentDeepEquals(other.grid)) return false
        if (placedWords != other.placedWords) return false
        if (level != other.level) return false
        if (gridSize != other.gridSize) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        var result = grid.contentDeepHashCode()
        result = 31 * result + placedWords.hashCode()
        result = 31 * result + level
        result = 31 * result + gridSize
        result = 31 * result + category.hashCode()
        return result
    }
}

data class PlacedWord(
    val word: String,
    val displayWord: String,
    val isReversed: Boolean,
    val startRow: Int,
    val startCol: Int,
    val direction: Direction
)

data class DifficultyConfig(
    val gridSize: Int,
    val allowedDirections: List<Direction>,
    val reverseWordProbability: Float,
    val minWordLength: Int,
    val targetWordCount: Int,
    val distractorLetters: String
)

data class Placement(
    val row: Int,
    val col: Int,
    val direction: Direction
)

enum class Direction(val deltas: Pair<Int, Int>) {
    HORIZONTAL(0 to 1),
    VERTICAL(1 to 0),
    DIAGONAL_DOWN_RIGHT(1 to 1),
    DIAGONAL_DOWN_LEFT(1 to -1),
    HORIZONTAL_REVERSE(0 to -1),
    VERTICAL_REVERSE(-1 to 0),
    DIAGONAL_UP_RIGHT(-1 to 1),
    DIAGONAL_UP_LEFT(-1 to -1)
}
