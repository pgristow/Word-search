package com.wordsearch.service

import com.wordsearch.repository.WordRepository
import com.wordsearch.repository.CategoryRepository
import org.springframework.stereotype.Service
import java.util.UUID
import kotlin.math.sqrt
import kotlin.random.Random

@Service
class GameBoardGenerator(
    private val wordRepository: WordRepository,
    private val categoryRepository: CategoryRepository
) {

    /**
     * Generates an endless word search board that scales with level
     */
    fun generateBoard(
        level: Int,
        words: List<String>,
        categoryName: String
    ): GameBoard = generateBoard(words, categoryName, getDifficultyConfig(level), level)

    /**
     * Generates a board with an explicit difficulty config. Used by casual mode,
     * which has a fixed, level-independent difficulty (see [getCasualConfig]).
     */
    fun generateBoard(
        words: List<String>,
        categoryName: String,
        config: DifficultyConfig,
        level: Int = 1
    ): GameBoard {
        val grid = Array(config.gridSize) { CharArray(config.gridSize) { ' ' } }
        val placedWords = mutableListOf<PlacedWord>()

        // Sort words by length (longest first for better placement)
        val sortedWords = words.take(config.targetWordCount).sortedByDescending { it.length }
        val diagonalDirs = config.allowedDirections.filter { it.isDiagonal }

        sortedWords.forEachIndexed { index, word ->
            val shouldReverse = Random.nextFloat() < config.reverseWordProbability
            val wordToPlace = if (shouldReverse) word.reversed() else word

            // Guarantee at least one diagonal per board: force the first (longest) word
            // onto a diagonal direction when the config allows diagonals. Subsequent words
            // prefer placements that cross an already-placed word at a shared letter
            // (crossword-style), packing the board with mixed directions.
            val forceDiagonal = index == 0 && diagonalDirs.isNotEmpty()
            val placement = if (forceDiagonal) {
                findInterlockingPlacement(grid, wordToPlace, config, preferIntersection = false, restrictTo = diagonalDirs)
                    ?: findInterlockingPlacement(grid, wordToPlace, config, preferIntersection = false)
            } else {
                findInterlockingPlacement(grid, wordToPlace, config, preferIntersection = placedWords.isNotEmpty())
            }
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
    fun getDifficultyConfig(level: Int): DifficultyConfig {
        val allDirs = Direction.values().toList()
        // Diagonals available even in the easiest band (we force one per board); reverse
        // ramps up across the three tiers. Grid size and word length grow with level.
        val easyDirs = listOf(
            Direction.HORIZONTAL,
            Direction.VERTICAL,
            Direction.DIAGONAL_DOWN_RIGHT,
            Direction.DIAGONAL_DOWN_LEFT
        )
        val mediumDirs = listOf(
            Direction.HORIZONTAL,
            Direction.VERTICAL,
            Direction.DIAGONAL_DOWN_RIGHT,
            Direction.DIAGONAL_DOWN_LEFT,
            Direction.HORIZONTAL_REVERSE,
            Direction.VERTICAL_REVERSE
        )
        return when {
            // ---- EASY tier: levels 1–100 ----
            level <= 25 -> DifficultyConfig(
                gridSize = 9,
                allowedDirections = easyDirs,
                reverseWordProbability = 0.1f,
                minWordLength = 3,
                targetWordCount = 5,
                distractorLetters = "ETAOINSHRDLU"
            )
            level <= 60 -> DifficultyConfig(
                gridSize = 10,
                allowedDirections = easyDirs,
                reverseWordProbability = 0.15f,
                minWordLength = 3,
                targetWordCount = 6,
                distractorLetters = "ETAOINSHRDLUCMFWYP"
            )
            level <= 100 -> DifficultyConfig(
                gridSize = 10,
                allowedDirections = mediumDirs,
                reverseWordProbability = 0.2f,
                minWordLength = 3,
                targetWordCount = 6,
                distractorLetters = "ETAOINSHRDLUCMFWYPVBGK"
            )
            // ---- MEDIUM tier: levels 101–400 ----
            level <= 200 -> DifficultyConfig(
                gridSize = 11,
                allowedDirections = mediumDirs,
                reverseWordProbability = 0.35f,
                minWordLength = 4,
                targetWordCount = 7,
                distractorLetters = "ETAOINSHRDLUCMFWYPVBGKJQXZ"
            )
            level <= 300 -> DifficultyConfig(
                gridSize = 12,
                allowedDirections = allDirs,
                reverseWordProbability = 0.45f,
                minWordLength = 4,
                targetWordCount = 8,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
            level <= 400 -> DifficultyConfig(
                gridSize = 12,
                allowedDirections = allDirs,
                reverseWordProbability = 0.55f,
                minWordLength = 4,
                targetWordCount = 9,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
            // ---- HARD tier: levels 401–1000 ----
            level <= 600 -> DifficultyConfig(
                gridSize = 13,
                allowedDirections = allDirs,
                reverseWordProbability = 0.65f,
                minWordLength = 5,
                targetWordCount = 10,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
            level <= 800 -> DifficultyConfig(
                gridSize = 14,
                allowedDirections = allDirs,
                reverseWordProbability = 0.75f,
                minWordLength = 5,
                targetWordCount = 11,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
            else -> DifficultyConfig(
                gridSize = 15,
                allowedDirections = allDirs,
                reverseWordProbability = 0.85f,
                minWordLength = 6,
                targetWordCount = 12,
                distractorLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            )
        }
    }

    /**
     * Fixed, easy difficulty for casual mode. Independent of the player's level so
     * casual always offers a relaxed baseline — but with a guaranteed diagonal and a
     * little reverse/diagonal variety so it doesn't feel monotonous.
     */
    fun getCasualConfig(): DifficultyConfig = DifficultyConfig(
        gridSize = 10,
        allowedDirections = listOf(
            Direction.HORIZONTAL,
            Direction.VERTICAL,
            Direction.DIAGONAL_DOWN_RIGHT,
            Direction.DIAGONAL_DOWN_LEFT
        ),
        reverseWordProbability = 0.15f,
        minWordLength = 3,
        targetWordCount = 8,
        distractorLetters = "ETAOINSHRDLUCMFWYPVBGKJQXZ"
    )

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
     * Enumerates every legal placement and picks one, preferring placements that cross an
     * already-placed word at a shared letter when [preferIntersection] is set. Produces an
     * interlocking, crossword-like board with mixed directions.
     */
    private fun findInterlockingPlacement(
        grid: Array<CharArray>,
        word: String,
        config: DifficultyConfig,
        preferIntersection: Boolean,
        restrictTo: List<Direction>? = null
    ): Placement? {
        val directions = restrictTo ?: config.allowedDirections
        val candidates = ArrayList<Pair<Placement, Int>>() // placement + number of shared (crossing) letters
        for (row in grid.indices) {
            for (col in grid[row].indices) {
                for (direction in directions) {
                    val overlap = overlapCount(grid, word, row, col, direction)
                    if (overlap >= 0) candidates.add(Placement(row, col, direction) to overlap)
                }
            }
        }
        if (candidates.isEmpty()) return null
        if (preferIntersection) {
            val crossing = candidates.filter { it.second > 0 }
            if (crossing.isNotEmpty()) return crossing.random().first
        }
        return candidates.random().first
    }

    /**
     * Number of cells of this placement that already hold the matching letter (i.e. real
     * intersections), or -1 if the word cannot be placed here at all.
     */
    private fun overlapCount(
        grid: Array<CharArray>,
        word: String,
        row: Int,
        col: Int,
        direction: Direction
    ): Int {
        val (dr, dc) = direction.deltas
        var overlap = 0
        for (i in word.indices) {
            val r = row + dr * i
            val c = col + dc * i
            if (r !in grid.indices || c !in grid[r].indices) return -1
            val cell = grid[r][c]
            if (cell != ' ' && cell != word[i]) return -1
            if (cell == word[i]) overlap++
        }
        return overlap
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

        // Reduced speed bonus: max 100 points for instant finds
        // Formula: (60 - timeElapsed) * 2, capped at 100
        val speedBonus = minOf(100, maxOf(0, (60 - timeElapsed) * 2))

        val comboMultiplier = when (currentCombo) {
            in 2..4 -> 2
            in 5..9 -> 3
            in 10..Int.MAX_VALUE -> 4
            else -> 1
        }

        return (baseScore + reverseBonus + diagonalBonus + speedBonus) * comboMultiplier
    }

    /**
     * Generates a board for a specific category with custom difficulty and word count
     * Used for daily challenges and boss levels
     */
    fun generateBoardForCategory(
        categoryId: UUID,
        difficultyLevel: Int,
        targetWordCount: Int
    ): GameBoard {
        // Get category
        val category = categoryRepository.findById(categoryId)
            .orElseThrow { IllegalArgumentException("Category not found") }

        // Get words for this category
        val words = wordRepository.findByCategoryId(categoryId)
            .map { it.word }

        if (words.isEmpty()) {
            throw IllegalStateException("No words found for category: ${category.name}")
        }

        // Get difficulty configuration
        val baseConfig = getDifficultyConfig(difficultyLevel)

        // Adjust target word count based on parameter
        val adjustedConfig = baseConfig.copy(targetWordCount = targetWordCount)

        // Generate grid
        val gridSize = adjustedConfig.gridSize
        val grid = Array(gridSize) { CharArray(gridSize) { ' ' } }
        val placedWords = mutableListOf<PlacedWord>()

        // Sort words by length (longest first for better placement)
        val sortedWords = words
            .shuffled()  // Randomize to get different words each time
            .take(targetWordCount)
            .sortedByDescending { it.length }

        for (word in sortedWords) {
            val shouldReverse = Random.nextFloat() < adjustedConfig.reverseWordProbability
            val wordToPlace = if (shouldReverse) word.reversed() else word

            val placement = findPlacement(grid, wordToPlace, adjustedConfig)
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
        fillEmptyCells(grid, adjustedConfig.distractorLetters)

        return GameBoard(
            grid = grid,
            placedWords = placedWords,
            level = difficultyLevel,
            gridSize = gridSize,
            category = category.name
        )
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
    /** Snapshot for persistence + server-authoritative validation. */
    fun toBoardState(): BoardState = BoardState(
        gridSize = gridSize,
        grid = grid.map { String(it) },
        solution = placedWords.map {
            SolutionWord(
                word = it.word.uppercase(),
                path = it.cellPath(),
                isReversed = it.isReversed,
                direction = it.direction.name
            )
        }
    )

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
) {
    /** The exact grid cells this word occupies, from start following the direction. */
    fun cellPath(): List<Cell> {
        val (dr, dc) = direction.deltas
        return displayWord.indices.map { i -> Cell(startRow + dr * i, startCol + dc * i) }
    }
}

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
    DIAGONAL_UP_LEFT(-1 to -1);

    /** True for the four diagonal directions. */
    val isDiagonal: Boolean
        get() = deltas.first != 0 && deltas.second != 0
}
