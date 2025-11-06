package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.service.UserProgressService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/user/progress")
class UserProgressController(
    private val userProgressService: UserProgressService
) {

    @GetMapping
    fun getUserProgress(@RequestParam userId: String): ResponseEntity<Any> {
        return try {
            val progress = userProgressService.getUserProgress(UUID.fromString(userId))
            ResponseEntity.ok(progress)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "User progress not found"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @GetMapping("/statistics")
    fun getUserStatistics(@RequestParam userId: String): ResponseEntity<Any> {
        return try {
            val stats = userProgressService.getUserStatistics(UUID.fromString(userId))
            ResponseEntity.ok(stats)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "User progress not found"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @PostMapping("/update-streak")
    fun updateStreak(@RequestParam userId: String): ResponseEntity<Any> {
        return try {
            userProgressService.updateStreak(UUID.fromString(userId))
            ResponseEntity.ok(mapOf("message" to "Streak updated successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse(e.message ?: "User progress not found"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @GetMapping("/leaderboard")
    fun getLeaderboard(@RequestParam(defaultValue = "100") limit: Int): ResponseEntity<Any> {
        return try {
            val leaderboard = userProgressService.getLeaderboard(limit)
            ResponseEntity.ok(leaderboard)
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }
}
