package com.wordsearch.service

import com.wordsearch.model.AdView
import com.wordsearch.model.AdSessionState
import com.wordsearch.repository.AdViewRepository
import com.wordsearch.repository.AdSessionStateRepository
import com.wordsearch.repository.PremiumSubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

data class AdShouldShowResponse(
    val shouldShowAd: Boolean,
    val reason: String,
    val adsRemaining: Int,
    val adsWatched: Int,
    val sessionStatus: String,
    val minutesUntilReset: Int?
)

data class AdViewResponse(
    val adRecorded: Boolean,
    val adsWatched: Int,
    val adsRemaining: Int,
    val adFreeHourStarted: Boolean,
    val message: String
)

data class AdSessionStatusResponse(
    val adsWatched: Int,
    val adsRemaining: Int,
    val isInAdFreeHour: Boolean,
    val minutesUntilReset: Int?,
    val sessionStartTime: LocalDateTime?,
    val sessionExpiresAt: LocalDateTime?
)

@Service
class AdService(
    private val adViewRepository: AdViewRepository,
    private val adSessionStateRepository: AdSessionStateRepository,
    private val premiumSubscriptionRepository: PremiumSubscriptionRepository
) {

    companion object {
        const val MAX_ADS_PER_HOUR = 5
        const val AD_FREE_DURATION_MINUTES = 60
        const val SESSION_DURATION_MINUTES = 60
    }

    /**
     * Check if user should see an ad
     */
    @Transactional
    fun shouldShowAd(userId: UUID): AdShouldShowResponse {
        // Check if user is premium
        val premium = premiumSubscriptionRepository.findByUserId(userId)
        if (premium?.isPremium == true) {
            return AdShouldShowResponse(
                shouldShowAd = false,
                reason = "User has premium subscription",
                adsRemaining = 0,
                adsWatched = 0,
                sessionStatus = "PREMIUM",
                minutesUntilReset = null
            )
        }

        // Get or create ad session
        val session = getOrCreateSession(userId)
        val now = LocalDateTime.now()

        // Check if session has expired
        if (now.isAfter(session.sessionExpiresAt)) {
            // Session expired, reset it
            val newSession = resetSession(userId)
            return AdShouldShowResponse(
                shouldShowAd = true,
                reason = "New ad session started",
                adsRemaining = newSession.adsRemaining,
                adsWatched = newSession.adsWatchedCount,
                sessionStatus = "ACTIVE",
                minutesUntilReset = SESSION_DURATION_MINUTES
            )
        }

        // Check if user has watched max ads and is in ad-free hour
        if (session.adsWatchedCount >= MAX_ADS_PER_HOUR) {
            val minutesUntilReset = java.time.Duration.between(now, session.sessionExpiresAt).toMinutes().toInt()
            return AdShouldShowResponse(
                shouldShowAd = false,
                reason = "User is in ad-free hour (watched $MAX_ADS_PER_HOUR ads)",
                adsRemaining = 0,
                adsWatched = session.adsWatchedCount,
                sessionStatus = "AD_FREE_HOUR",
                minutesUntilReset = minutesUntilReset
            )
        }

        // User can see more ads
        val minutesUntilReset = java.time.Duration.between(now, session.sessionExpiresAt).toMinutes().toInt()
        return AdShouldShowResponse(
            shouldShowAd = true,
            reason = "User can watch ${session.adsRemaining} more ads",
            adsRemaining = session.adsRemaining,
            adsWatched = session.adsWatchedCount,
            sessionStatus = "ACTIVE",
            minutesUntilReset = minutesUntilReset
        )
    }

    /**
     * Record an ad view
     */
    @Transactional
    fun recordAdView(
        userId: UUID,
        adType: String? = "INTERSTITIAL",
        adUnitId: String? = null,
        sessionId: String? = null
    ): AdViewResponse {
        // Check if user is premium (shouldn't happen, but handle it)
        val premium = premiumSubscriptionRepository.findByUserId(userId)
        if (premium?.isPremium == true) {
            return AdViewResponse(
                adRecorded = false,
                adsWatched = 0,
                adsRemaining = 0,
                adFreeHourStarted = false,
                message = "Premium users don't see ads"
            )
        }

        // Get or create session
        val session = getOrCreateSession(userId)
        val now = LocalDateTime.now()

        // Check if session expired
        if (now.isAfter(session.sessionExpiresAt)) {
            // Reset session
            val newSession = resetSession(userId)
            // Record the ad view
            recordAdViewInDatabase(userId, adType, adUnitId, sessionId)
            // Update session
            val updatedSession = incrementAdCount(newSession)

            return AdViewResponse(
                adRecorded = true,
                adsWatched = updatedSession.adsWatchedCount,
                adsRemaining = updatedSession.adsRemaining,
                adFreeHourStarted = false,
                message = "Ad recorded (${updatedSession.adsWatchedCount}/$MAX_ADS_PER_HOUR)"
            )
        }

        // Check if already at max ads
        if (session.adsWatchedCount >= MAX_ADS_PER_HOUR) {
            return AdViewResponse(
                adRecorded = false,
                adsWatched = session.adsWatchedCount,
                adsRemaining = 0,
                adFreeHourStarted = true,
                message = "User is already in ad-free hour"
            )
        }

        // Record the ad view
        recordAdViewInDatabase(userId, adType, adUnitId, sessionId)

        // Increment ad count
        val updatedSession = incrementAdCount(session)

        // Check if this was the last ad before ad-free hour
        val adFreeHourStarted = updatedSession.adsWatchedCount >= MAX_ADS_PER_HOUR

        return AdViewResponse(
            adRecorded = true,
            adsWatched = updatedSession.adsWatchedCount,
            adsRemaining = updatedSession.adsRemaining,
            adFreeHourStarted = adFreeHourStarted,
            message = if (adFreeHourStarted) {
                "Ad-free hour started! No more ads for 60 minutes"
            } else {
                "Ad recorded (${updatedSession.adsWatchedCount}/$MAX_ADS_PER_HOUR)"
            }
        )
    }

    /**
     * Get current ad session status
     */
    fun getSessionStatus(userId: UUID): AdSessionStatusResponse {
        // Check if user is premium
        val premium = premiumSubscriptionRepository.findByUserId(userId)
        if (premium?.isPremium == true) {
            return AdSessionStatusResponse(
                adsWatched = 0,
                adsRemaining = 0,
                isInAdFreeHour = false,
                minutesUntilReset = null,
                sessionStartTime = null,
                sessionExpiresAt = null
            )
        }

        val session = getOrCreateSession(userId)
        val now = LocalDateTime.now()

        // Check if session expired
        if (now.isAfter(session.sessionExpiresAt)) {
            return AdSessionStatusResponse(
                adsWatched = 0,
                adsRemaining = MAX_ADS_PER_HOUR,
                isInAdFreeHour = false,
                minutesUntilReset = SESSION_DURATION_MINUTES,
                sessionStartTime = now,
                sessionExpiresAt = now.plusMinutes(SESSION_DURATION_MINUTES.toLong())
            )
        }

        val minutesUntilReset = java.time.Duration.between(now, session.sessionExpiresAt).toMinutes().toInt()
        val isInAdFreeHour = session.adsWatchedCount >= MAX_ADS_PER_HOUR

        return AdSessionStatusResponse(
            adsWatched = session.adsWatchedCount,
            adsRemaining = session.adsRemaining,
            isInAdFreeHour = isInAdFreeHour,
            minutesUntilReset = minutesUntilReset,
            sessionStartTime = session.sessionStartTime,
            sessionExpiresAt = session.sessionExpiresAt
        )
    }

    /**
     * Get or create ad session for user
     */
    private fun getOrCreateSession(userId: UUID): AdSessionState {
        val existingSession = adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(userId)
        val now = LocalDateTime.now()

        // If no session or session expired, create new one
        if (existingSession == null || now.isAfter(existingSession.sessionExpiresAt)) {
            return createNewSession(userId)
        }

        return existingSession
    }

    /**
     * Create a new ad session
     */
    @Transactional
    fun createNewSession(userId: UUID): AdSessionState {
        val now = LocalDateTime.now()
        val session = AdSessionState(
            userId = userId,
            sessionStartTime = now,
            adsWatchedCount = 0,
            adsRemaining = MAX_ADS_PER_HOUR,
            sessionExpiresAt = now.plusMinutes(SESSION_DURATION_MINUTES.toLong()),
            createdAt = now,
            updatedAt = now
        )
        return adSessionStateRepository.save(session)
    }

    /**
     * Reset expired session
     */
    @Transactional
    fun resetSession(userId: UUID): AdSessionState {
        return createNewSession(userId)
    }

    /**
     * Increment ad count in session
     */
    @Transactional
    fun incrementAdCount(session: AdSessionState): AdSessionState {
        val newCount = session.adsWatchedCount + 1
        val updated = session.copy(
            adsWatchedCount = newCount,
            adsRemaining = MAX_ADS_PER_HOUR - newCount,
            updatedAt = LocalDateTime.now()
        )
        return adSessionStateRepository.save(updated)
    }

    /**
     * Record ad view in database
     */
    private fun recordAdViewInDatabase(
        userId: UUID,
        adType: String?,
        adUnitId: String?,
        sessionId: String?
    ) {
        val adView = AdView(
            userId = userId,
            viewedAt = LocalDateTime.now(),
            adType = adType,
            adUnitId = adUnitId,
            sessionId = sessionId
        )
        adViewRepository.save(adView)
    }

    /**
     * Get ad view statistics
     */
    fun getAdStatistics(userId: UUID, daysBack: Int = 30): Map<String, Any> {
        val startDate = LocalDateTime.now().minusDays(daysBack.toLong())
        val adViews = adViewRepository.findByUserIdAndViewedAtAfter(userId, startDate)

        val totalAds = adViews.size
        val adsByType = adViews.groupBy { it.adType }.mapValues { it.value.size }
        val averageAdsPerDay = if (daysBack > 0) totalAds.toDouble() / daysBack else 0.0

        return mapOf(
            "totalAdsWatched" to totalAds,
            "adsByType" to adsByType,
            "averageAdsPerDay" to averageAdsPerDay,
            "periodDays" to daysBack,
            "startDate" to startDate,
            "endDate" to LocalDateTime.now()
        )
    }
}
