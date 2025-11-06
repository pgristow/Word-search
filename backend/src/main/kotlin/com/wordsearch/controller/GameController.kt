package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.service.GameBoardGenerator
import com.wordsearch.repository.CategoryRepository
import com.wordsearch.repository.WordRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/game")
class GameController(
    private val gameBoardGenerator: GameBoardGenerator,
    private val categoryRepository: CategoryRepository,
    private val wordRepository: WordRepository
) {

    @PostMapping("/start")
    fun startGame(
        @RequestParam categoryId: String,
        @RequestParam level: Int = 1
    ): ResponseEntity<Any> {
        return try {
            val category = categoryRepository.findById(UUID.fromString(categoryId))
                .orElseThrow { IllegalArgumentException("Category not found") }

            // Get words for this category
            val allWords = wordRepository.findByCategoryId(category.id)
            val wordsList = allWords.map { it.word }

            if (wordsList.isEmpty()) {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse("No words found for this category"))
            }

            // Generate game board
            val gameBoard = gameBoardGenerator.generateBoard(
                level = level,
                words = wordsList,
                categoryName = category.name
            )

            // Convert to response DTO
            val response = mapOf(
                "level" to gameBoard.level,
                "gridSize" to gameBoard.gridSize,
                "category" to gameBoard.category,
                "grid" to gameBoard.grid.map { it.joinToString("") },
                "words" to gameBoard.placedWords.map {
                    mapOf(
                        "word" to it.word,
                        "isReversed" to it.isReversed
                    )
                }
            )

            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Failed to start game"))
        }
    }

    @PostMapping("/calculate-score")
    fun calculateScore(
        @RequestParam word: String,
        @RequestParam isReversed: Boolean = false,
        @RequestParam isDiagonal: Boolean = false,
        @RequestParam timeElapsed: Int = 0,
        @RequestParam currentCombo: Int = 0
    ): Map<String, Any> {
        val score = gameBoardGenerator.calculateWordScore(
            word = word,
            isReversed = isReversed,
            isDiagonal = isDiagonal,
            timeElapsed = timeElapsed,
            currentCombo = currentCombo
        )

        return mapOf(
            "score" to score,
            "word" to word,
            "baseScore" to (100 * word.length),
            "bonuses" to mapOf(
                "reversed" to if (isReversed) (100 * word.length * 0.5).toInt() else 0,
                "diagonal" to if (isDiagonal) (100 * word.length * 0.25).toInt() else 0,
                "speed" to maxOf(0, (60 - timeElapsed) * 10),
                "comboMultiplier" to when (currentCombo) {
                    in 2..4 -> 2
                    in 5..9 -> 3
                    in 10..Int.MAX_VALUE -> 4
                    else -> 1
                }
            )
        )
    }

    @GetMapping("/level-from-score")
    fun getLevelFromScore(@RequestParam totalScore: Long): Map<String, Any> {
        val level = gameBoardGenerator.calculateLevel(totalScore)
        return mapOf(
            "level" to level,
            "totalScore" to totalScore
        )
    }
}
