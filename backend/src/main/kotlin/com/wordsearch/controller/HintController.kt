package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.service.GameSessionService
import com.wordsearch.service.InsufficientCoinsException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Reveals one unfound target word in exchange for coins.
 * Mapped at /api/sessions/{sessionId}/hint to match the client contract — kept in its
 * own controller because GameSessionController is based at /api/game/session.
 */
@RestController
@RequestMapping("/api/sessions")
class HintController(
    private val gameSessionService: GameSessionService
) {

    @PostMapping("/{sessionId}/hint")
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
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Insufficient coins"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to use hint"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }
}
