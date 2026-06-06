package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.dto.StartSessionRequest
import com.wordsearch.dto.SubmitWordRequest
import com.wordsearch.model.GameMode
import com.wordsearch.service.EconomyService
import com.wordsearch.service.GameSessionService
import com.wordsearch.service.InsufficientCoinsException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/game/session")
class GameSessionController(
    private val gameSessionService: GameSessionService
) {

    @PostMapping("/start")
    fun startSession(
        @Valid @RequestBody request: StartSessionRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
            val gameMode = try {
                GameMode.valueOf(request.gameMode.uppercase())
            } catch (e: IllegalArgumentException) {
                GameMode.CLASSIC // Default to CLASSIC if invalid mode
            }
            val response = gameSessionService.startNewSession(
                userId = UUID.fromString(userId),
                categoryId = UUID.fromString(request.categoryId),
                gameMode = gameMode
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
        @Valid @RequestBody request: SubmitWordRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
            val response = gameSessionService.submitWord(
                sessionId = UUID.fromString(sessionId),
                userId = UUID.fromString(userId),
                word = request.word.uppercase(),
                isReversed = request.isReversed,
                isDiagonal = request.isDiagonal,
                timeElapsed = request.timeElapsed,
                path = request.path
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

    // Reveals one unfound target word, charging coins. Mapped at the spec's
    // absolute path (/api/sessions/...) which intentionally differs from this
    // controller's /api/game/session base for the client contract.
    @PostMapping("/api/sessions/{sessionId}/hint")
    fun useHint(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
            val response = gameSessionService.useHint(
                sessionId = UUID.fromString(sessionId),
                userId = UUID.fromString(userId)
            )
            ResponseEntity.ok(response)
        } catch (e: InsufficientCoinsException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Insufficient coins"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to use hint"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @PostMapping("/{sessionId}/end")
    fun endSession(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
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
    fun getActiveSession(authentication: Authentication): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
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

    @PostMapping("/{sessionId}/save")
    fun saveCasualProgress(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
            val summary = gameSessionService.saveCasualProgress(
                sessionId = UUID.fromString(sessionId),
                userId = UUID.fromString(userId)
            )
            ResponseEntity.ok(summary)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to save progress"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @PostMapping("/{sessionId}/resume")
    fun resumeCasualGame(
        @PathVariable sessionId: String,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
            val session = gameSessionService.resumeCasualGame(
                sessionId = UUID.fromString(sessionId),
                userId = UUID.fromString(userId)
            )
            if (session != null) {
                ResponseEntity.ok(session)
            } else {
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse("Session not found"))
            }
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to resume game"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }
}
