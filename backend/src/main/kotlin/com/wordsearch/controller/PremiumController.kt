package com.wordsearch.controller

import com.wordsearch.service.PremiumService
import com.wordsearch.service.PremiumPurchaseRequest
import com.wordsearch.service.PremiumPurchaseResponse
import com.wordsearch.service.PremiumStatusResponse
import com.wordsearch.service.PremiumRestoreResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class RestorePurchaseRequest(
    val purchaseToken: String
)

@RestController
@RequestMapping("/api/premium")
class PremiumController(
    private val premiumService: PremiumService
) {

    /**
     * Process a premium purchase
     * POST /api/premium/purchase
     */
    @PostMapping("/purchase")
    fun processPurchase(
        @RequestBody request: PremiumPurchaseRequest,
        authentication: Authentication
    ): ResponseEntity<PremiumPurchaseResponse> {
        val userId = UUID.fromString(authentication.name)
        val response = premiumService.processPurchase(userId, request)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    /**
     * Get user's premium status
     * GET /api/premium/status
     */
    @GetMapping("/status")
    fun getPremiumStatus(authentication: Authentication): ResponseEntity<PremiumStatusResponse> {
        val userId = UUID.fromString(authentication.name)
        val status = premiumService.getPremiumStatus(userId)
        return ResponseEntity.ok(status)
    }

    /**
     * Restore previous purchase (for device changes)
     * POST /api/premium/restore
     */
    @PostMapping("/restore")
    fun restorePurchase(
        @RequestBody request: RestorePurchaseRequest,
        authentication: Authentication
    ): ResponseEntity<PremiumRestoreResponse> {
        val userId = UUID.fromString(authentication.name)
        val response = premiumService.restorePurchase(userId, request.purchaseToken)
        return if (response.success) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    /**
     * Cancel auto-renewal
     * DELETE /api/premium/cancel-renewal
     */
    @DeleteMapping("/cancel-renewal")
    fun cancelAutoRenewal(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val userId = UUID.fromString(authentication.name)
        val response = premiumService.cancelAutoRenewal(userId)
        return if (response["success"] == true) {
            ResponseEntity.ok(response)
        } else {
            ResponseEntity.badRequest().body(response)
        }
    }

    /**
     * Get premium statistics (admin endpoint)
     * GET /api/premium/statistics
     */
    @GetMapping("/statistics")
    fun getPremiumStatistics(): ResponseEntity<Map<String, Any>> {
        val stats = premiumService.getPremiumStatistics()
        return ResponseEntity.ok(stats)
    }

    /**
     * Check and update expired subscriptions (admin/cron endpoint)
     * POST /api/premium/check-expired
     */
    @PostMapping("/check-expired")
    fun checkExpiredSubscriptions(): ResponseEntity<Map<String, Int>> {
        val result = premiumService.checkExpiredSubscriptions()
        return ResponseEntity.ok(result)
    }
}
