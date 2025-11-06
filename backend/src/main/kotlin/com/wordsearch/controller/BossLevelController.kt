package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.service.BossLevelService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/boss")
class BossLevelController(
    private val bossLevelService: BossLevelService
) {

    @GetMapping("/is-boss-level")
    fun isBossLevel(@RequestParam level: Int): Map<String, Any> {
        val isBoss = bossLevelService.isBossLevel(level)
        val response = mutableMapOf<String, Any>(
            "level" to level,
            "isBossLevel" to isBoss
        )

        if (isBoss) {
            response["bossType"] = bossLevelService.getBossType(level).name
        }

        return response
    }

    @PostMapping("/start")
    fun startBossLevel(
        @RequestParam userId: String,
        @RequestParam sessionId: String,
        @RequestParam level: Int,
        @RequestParam categoryId: String
    ): ResponseEntity<Any> {
        return try {
            val response = bossLevelService.startBossLevel(
                userId = UUID.fromString(userId),
                sessionId = UUID.fromString(sessionId),
                level = level,
                categoryId = UUID.fromString(categoryId)
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to start boss level"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @PostMapping("/{attemptId}/shuffle")
    fun shuffleBoard(
        @PathVariable attemptId: String,
        @RequestParam userId: String,
        @RequestBody foundWords: List<String>
    ): ResponseEntity<Any> {
        return try {
            val response = bossLevelService.shuffleBoard(
                attemptId = UUID.fromString(attemptId),
                userId = UUID.fromString(userId),
                foundWords = foundWords
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to shuffle board"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @PostMapping("/{attemptId}/complete")
    fun completeBossLevel(
        @PathVariable attemptId: String,
        @RequestParam userId: String,
        @RequestParam wordsFound: Int,
        @RequestParam timeTaken: Int
    ): ResponseEntity<Any> {
        return try {
            val response = bossLevelService.completeBossLevel(
                attemptId = UUID.fromString(attemptId),
                userId = UUID.fromString(userId),
                wordsFound = wordsFound,
                timeTaken = timeTaken
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to complete boss level"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @GetMapping("/statistics")
    fun getBossStatistics(@RequestParam userId: String): ResponseEntity<Any> {
        return try {
            val stats = bossLevelService.getBossStatistics(UUID.fromString(userId))
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }
}
