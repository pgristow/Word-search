package com.wordsearch.controller

import com.wordsearch.service.AdService
import com.wordsearch.service.AdShouldShowResponse
import com.wordsearch.service.AdViewResponse
import com.wordsearch.service.AdSessionStatusResponse
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class RecordAdRequest(
    val adType: String? = "INTERSTITIAL",
    val adUnitId: String? = null,
    val sessionId: String? = null
)

@RestController
@RequestMapping("/api/ads")
class AdController(
    private val adService: AdService
) {

    /**
     * Check if user should see an ad
     * GET /api/ads/should-show
     */
    @GetMapping("/should-show")
    fun shouldShowAd(authentication: Authentication): ResponseEntity<AdShouldShowResponse> {
        val userId = UUID.fromString(authentication.principal as String)
        val response = adService.shouldShowAd(userId)
        return ResponseEntity.ok(response)
    }

    /**
     * Record an ad view
     * POST /api/ads/view
     */
    @PostMapping("/view")
    fun recordAdView(
        @RequestBody request: RecordAdRequest,
        authentication: Authentication
    ): ResponseEntity<AdViewResponse> {
        val userId = UUID.fromString(authentication.principal as String)
        val response = adService.recordAdView(
            userId = userId,
            adType = request.adType,
            adUnitId = request.adUnitId,
            sessionId = request.sessionId
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Get current ad session status
     * GET /api/ads/session-status
     */
    @GetMapping("/session-status")
    fun getSessionStatus(authentication: Authentication): ResponseEntity<AdSessionStatusResponse> {
        val userId = UUID.fromString(authentication.principal as String)
        val response = adService.getSessionStatus(userId)
        return ResponseEntity.ok(response)
    }

    /**
     * Get ad statistics
     * GET /api/ads/statistics
     */
    @GetMapping("/statistics")
    fun getAdStatistics(
        @RequestParam(defaultValue = "30") daysBack: Int,
        authentication: Authentication
    ): ResponseEntity<Map<String, Any>> {
        val userId = UUID.fromString(authentication.principal as String)
        val stats = adService.getAdStatistics(userId, daysBack)
        return ResponseEntity.ok(stats)
    }

    /**
     * Reset ad session (for testing purposes)
     * POST /api/ads/reset-session
     */
    @PostMapping("/reset-session")
    fun resetSession(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val userId = UUID.fromString(authentication.principal as String)
        val newSession = adService.resetSession(userId)
        return ResponseEntity.ok(
            mapOf(
                "success" to true,
                "message" to "Ad session reset successfully",
                "adsRemaining" to newSession.adsRemaining,
                "sessionExpiresAt" to newSession.sessionExpiresAt
            )
        )
    }
}
