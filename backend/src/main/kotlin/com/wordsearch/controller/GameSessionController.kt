package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.service.GameSessionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/game/session")
class GameSessionController(
    private val gameSessionService: GameSessionService
) {

    @PostMapping("/start")
    fun startSession(
        @RequestParam userId: String,
        @RequestParam categoryId: String
    ): ResponseEntity<Any> {
        return try {
            val response = gameSessionService.startNewSession(
                userId = UUID.fromString(userId),
                categoryId = UUID.fromString(categoryId)
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to start session"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @PostMapping("/{sessionId}/submit-word")
    fun submitWord(
        @PathVariable sessionId: String,
        @RequestParam userId: String,
        @RequestParam word: String,
        @RequestParam(defaultValue = "false") isReversed: Boolean,
        @RequestParam(defaultValue = "false") isDiagonal: Boolean,
        @RequestParam(defaultValue = "0") timeElapsed: Int
    ): ResponseEntity<Any> {
        return try {
            val response = gameSessionService.submitWord(
                sessionId = UUID.fromString(sessionId),
                userId = UUID.fromString(userId),
                word = word.uppercase(),
                isReversed = isReversed,
                isDiagonal = isDiagonal,
                timeElapsed = timeElapsed
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to submit word"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @PostMapping("/{sessionId}/end")
    fun endSession(
        @PathVariable sessionId: String,
        @RequestParam userId: String
    ): ResponseEntity<Any> {
        return try {
            val summary = gameSessionService.endSession(
                sessionId = UUID.fromString(sessionId),
                userId = UUID.fromString(userId)
            )
            ResponseEntity.ok(summary)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to end session"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @GetMapping("/active")
    fun getActiveSession(@RequestParam userId: String): ResponseEntity<Any> {
        return try {
            val session = gameSessionService.getActiveSession(UUID.fromString(userId))
            if (session != null) {
                ResponseEntity.ok(session)
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse("No active session found"))
            }
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }
}
