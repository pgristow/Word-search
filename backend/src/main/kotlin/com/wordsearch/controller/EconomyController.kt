package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.dto.PurchaseItemRequest
import com.wordsearch.service.EconomyService
import com.wordsearch.service.StoreService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/economy")
class EconomyController(
    private val economyService: EconomyService,
    private val storeService: StoreService
) {

    @GetMapping("/store")
    fun getStore(authentication: Authentication): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
            ResponseEntity.ok(storeService.store(UUID.fromString(userId)))
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to load store"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @PostMapping("/purchase")
    fun purchase(
        @RequestBody request: PurchaseItemRequest,
        authentication: Authentication
    ): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
            val result = storeService.purchase(
                userId = UUID.fromString(userId),
                itemType = request.itemType,
                itemId = UUID.fromString(request.itemId)
            )
            ResponseEntity.ok(result)
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to purchase"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }

    @GetMapping("/wallet")
    fun getWallet(authentication: Authentication): ResponseEntity<Any> {
        return try {
            val userId = authentication.principal as String
            val balance = economyService.balance(UUID.fromString(userId))
            ResponseEntity.ok(mapOf("coinBalance" to balance))
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Failed to load wallet"))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Internal server error"))
        }
    }
}
