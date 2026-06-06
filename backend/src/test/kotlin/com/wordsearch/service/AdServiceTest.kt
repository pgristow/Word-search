package com.wordsearch.service

import com.wordsearch.model.AdSessionState
import com.wordsearch.model.PremiumSubscription
import com.wordsearch.repository.AdSessionStateRepository
import com.wordsearch.repository.AdViewRepository
import com.wordsearch.repository.PremiumSubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class AdServiceTest {

    private lateinit var adViewRepository: AdViewRepository
    private lateinit var adSessionStateRepository: AdSessionStateRepository
    private lateinit var premiumSubscriptionRepository: PremiumSubscriptionRepository
    private lateinit var adService: AdService

    private val testUserId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        adViewRepository = mockk(relaxed = true)
        adSessionStateRepository = mockk(relaxed = true)
        premiumSubscriptionRepository = mockk(relaxed = true)
        adService = AdService(adViewRepository, adSessionStateRepository, premiumSubscriptionRepository)
    }

    @Test
    fun `test shouldShowAd returns false for premium users`() {
        val premium = PremiumSubscription(
            userId = testUserId,
            isPremium = true,
            purchaseDate = LocalDateTime.now()
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns premium

        val response = adService.shouldShowAd(testUserId)

        assertFalse(response.shouldShowAd)
        assertEquals("User has premium subscription", response.reason)
        assertEquals("PREMIUM", response.sessionStatus)
        assertEquals(0, response.adsRemaining)
    }

    @Test
    fun `test shouldShowAd returns true for new session`() {
        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(testUserId) } returns null
        every { adSessionStateRepository.save(any()) } answers { firstArg() }

        val response = adService.shouldShowAd(testUserId)

        assertTrue(response.shouldShowAd)
        assertEquals(5, response.adsRemaining)
        assertEquals(0, response.adsWatched)
        assertEquals("ACTIVE", response.sessionStatus)
    }

    @Test
    fun `test shouldShowAd returns false during ad-free hour`() {
        val now = LocalDateTime.now()
        val session = AdSessionState(
            userId = testUserId,
            sessionStartTime = now.minusMinutes(10),
            adsWatchedCount = 5,
            adsRemaining = 0,
            sessionExpiresAt = now.plusMinutes(50)
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(testUserId) } returns session

        val response = adService.shouldShowAd(testUserId)

        assertFalse(response.shouldShowAd)
        assertEquals("AD_FREE_HOUR", response.sessionStatus)
        assertEquals(0, response.adsRemaining)
        assertEquals(5, response.adsWatched)
        assertTrue(response.minutesUntilReset!! > 0)
    }

    @Test
    fun `test shouldShowAd returns true when session has remaining ads`() {
        val now = LocalDateTime.now()
        val session = AdSessionState(
            userId = testUserId,
            sessionStartTime = now.minusMinutes(5),
            adsWatchedCount = 3,
            adsRemaining = 2,
            sessionExpiresAt = now.plusMinutes(55)
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(testUserId) } returns session

        val response = adService.shouldShowAd(testUserId)

        assertTrue(response.shouldShowAd)
        assertEquals("ACTIVE", response.sessionStatus)
        assertEquals(2, response.adsRemaining)
        assertEquals(3, response.adsWatched)
    }

    @Test
    fun `test shouldShowAd creates new session when session expired`() {
        val now = LocalDateTime.now()
        val expiredSession = AdSessionState(
            userId = testUserId,
            sessionStartTime = now.minusHours(2),
            adsWatchedCount = 5,
            adsRemaining = 0,
            sessionExpiresAt = now.minusHours(1)
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(testUserId) } returns expiredSession
        every { adSessionStateRepository.save(any()) } answers { firstArg() }

        val response = adService.shouldShowAd(testUserId)

        assertTrue(response.shouldShowAd)
        assertEquals(5, response.adsRemaining)
        assertEquals(0, response.adsWatched)
        verify { adSessionStateRepository.save(any()) }
    }

    @Test
    fun `test recordAdView increments ad count`() {
        val now = LocalDateTime.now()
        val session = AdSessionState(
            userId = testUserId,
            sessionStartTime = now.minusMinutes(5),
            adsWatchedCount = 2,
            adsRemaining = 3,
            sessionExpiresAt = now.plusMinutes(55)
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(testUserId) } returns session
        every { adSessionStateRepository.save(any()) } answers { firstArg() }
        every { adViewRepository.save(any()) } answers { firstArg() }

        val response = adService.recordAdView(testUserId)

        assertTrue(response.adRecorded)
        assertEquals(3, response.adsWatched)
        assertEquals(2, response.adsRemaining)
        assertFalse(response.adFreeHourStarted)
        verify { adViewRepository.save(any()) }
        verify { adSessionStateRepository.save(any()) }
    }

    @Test
    fun `test recordAdView starts ad-free hour after 5th ad`() {
        val now = LocalDateTime.now()
        val session = AdSessionState(
            userId = testUserId,
            sessionStartTime = now.minusMinutes(10),
            adsWatchedCount = 4,
            adsRemaining = 1,
            sessionExpiresAt = now.plusMinutes(50)
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(testUserId) } returns session
        every { adSessionStateRepository.save(any()) } answers { firstArg() }
        every { adViewRepository.save(any()) } answers { firstArg() }

        val response = adService.recordAdView(testUserId)

        assertTrue(response.adRecorded)
        assertEquals(5, response.adsWatched)
        assertEquals(0, response.adsRemaining)
        assertTrue(response.adFreeHourStarted)
        assertTrue(response.message.contains("Ad-free hour started"))
    }

    @Test
    fun `test recordAdView does not record for premium users`() {
        val premium = PremiumSubscription(
            userId = testUserId,
            isPremium = true
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns premium

        val response = adService.recordAdView(testUserId)

        assertFalse(response.adRecorded)
        assertEquals(0, response.adsWatched)
        assertEquals(0, response.adsRemaining)
        assertTrue(response.message.contains("Premium"))
        verify(exactly = 0) { adViewRepository.save(any()) }
    }

    @Test
    fun `test getSessionStatus returns correct status for active session`() {
        val now = LocalDateTime.now()
        val session = AdSessionState(
            userId = testUserId,
            sessionStartTime = now.minusMinutes(15),
            adsWatchedCount = 3,
            adsRemaining = 2,
            sessionExpiresAt = now.plusMinutes(45)
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(testUserId) } returns session

        val response = adService.getSessionStatus(testUserId)

        assertEquals(3, response.adsWatched)
        assertEquals(2, response.adsRemaining)
        assertFalse(response.isInAdFreeHour)
        assertTrue(response.minutesUntilReset!! > 0)
    }

    @Test
    fun `test getSessionStatus returns ad-free hour status`() {
        val now = LocalDateTime.now()
        val session = AdSessionState(
            userId = testUserId,
            sessionStartTime = now.minusMinutes(20),
            adsWatchedCount = 5,
            adsRemaining = 0,
            sessionExpiresAt = now.plusMinutes(40)
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { adSessionStateRepository.findTopByUserIdOrderBySessionStartTimeDesc(testUserId) } returns session

        val response = adService.getSessionStatus(testUserId)

        assertEquals(5, response.adsWatched)
        assertEquals(0, response.adsRemaining)
        assertTrue(response.isInAdFreeHour)
    }

    @Test
    fun `test getSessionStatus returns premium status`() {
        val premium = PremiumSubscription(
            userId = testUserId,
            isPremium = true
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns premium

        val response = adService.getSessionStatus(testUserId)

        assertEquals(0, response.adsWatched)
        assertEquals(0, response.adsRemaining)
        assertFalse(response.isInAdFreeHour)
        assertNull(response.minutesUntilReset)
    }

    @Test
    fun `test createNewSession creates session with correct values`() {
        every { adSessionStateRepository.save(any()) } answers { firstArg() }

        val session = adService.createNewSession(testUserId)

        assertEquals(testUserId, session.userId)
        assertEquals(0, session.adsWatchedCount)
        assertEquals(5, session.adsRemaining)
        assertTrue(session.sessionExpiresAt.isAfter(LocalDateTime.now()))
    }

    @Test
    fun `test resetSession creates new session`() {
        every { adSessionStateRepository.save(any()) } answers { firstArg() }

        val session = adService.resetSession(testUserId)

        assertEquals(0, session.adsWatchedCount)
        assertEquals(5, session.adsRemaining)
        verify { adSessionStateRepository.save(any()) }
    }

    @Test
    fun `test incrementAdCount updates session correctly`() {
        val now = LocalDateTime.now()
        val session = AdSessionState(
            userId = testUserId,
            sessionStartTime = now,
            adsWatchedCount = 2,
            adsRemaining = 3,
            sessionExpiresAt = now.plusMinutes(60)
        )

        every { adSessionStateRepository.save(any()) } answers { firstArg() }

        val updated = adService.incrementAdCount(session)

        assertEquals(3, updated.adsWatchedCount)
        assertEquals(2, updated.adsRemaining)
        verify { adSessionStateRepository.save(any()) }
    }

    @Test
    fun `test getAdStatistics returns correct stats`() {
        val adViews = listOf<com.wordsearch.model.AdView>(
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true)
        )

        every { adViewRepository.findByUserIdAndViewedAtAfter(testUserId, any()) } returns adViews

        val stats = adService.getAdStatistics(testUserId, 30)

        assertEquals(3, stats["totalAdsWatched"])
        assertEquals(30, stats["periodDays"])
        assertTrue(stats.containsKey("averageAdsPerDay"))
    }
}
