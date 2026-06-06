package com.wordsearch.service

import com.wordsearch.model.PremiumSubscription
import com.wordsearch.model.User
import com.wordsearch.repository.PremiumSubscriptionRepository
import com.wordsearch.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class PremiumServiceTest {

    private lateinit var premiumSubscriptionRepository: PremiumSubscriptionRepository
    private lateinit var userRepository: UserRepository
    private lateinit var premiumService: PremiumService

    private val testUserId = UUID.randomUUID()
    private val testPurchaseToken = "test_purchase_token_12345"

    @BeforeEach
    fun setup() {
        premiumSubscriptionRepository = mockk(relaxed = true)
        userRepository = mockk(relaxed = true)
        premiumService = PremiumService(premiumSubscriptionRepository, userRepository)
    }

    @Test
    fun `test processPurchase creates new premium subscription for lifetime purchase`() {
        val user = User(
            id = testUserId,
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            isPremium = false
        )

        val request = PremiumPurchaseRequest(
            purchaseToken = testPurchaseToken,
            purchasePlatform = "GOOGLE_PLAY",
            isSubscription = false
        )

        every { userRepository.findById(testUserId) } returns Optional.of(user)
        every { premiumSubscriptionRepository.findByPurchaseToken(testPurchaseToken) } returns null
        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { premiumSubscriptionRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(any()) } answers { firstArg() }

        val response = premiumService.processPurchase(testUserId, request)

        assertTrue(response.success)
        assertTrue(response.isPremium)
        assertNotNull(response.purchaseDate)
        assertNotNull(response.expiryDate)
        assertTrue(response.message.contains("lifetime"))
        verify { premiumSubscriptionRepository.save(any()) }
        verify { userRepository.save(any()) }
    }

    @Test
    fun `test processPurchase creates monthly subscription`() {
        val user = User(
            id = testUserId,
            username = "testuser",
            email = "test@example.com",
            passwordHash = "hashedpassword",
            isPremium = false
        )

        val request = PremiumPurchaseRequest(
            purchaseToken = testPurchaseToken,
            purchasePlatform = "GOOGLE_PLAY",
            isSubscription = true
        )

        every { userRepository.findById(testUserId) } returns Optional.of(user)
        every { premiumSubscriptionRepository.findByPurchaseToken(testPurchaseToken) } returns null
        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null
        every { premiumSubscriptionRepository.save(any()) } answers { firstArg() }
        every { userRepository.save(any()) } answers { firstArg() }

        val response = premiumService.processPurchase(testUserId, request)

        assertTrue(response.success)
        assertTrue(response.isPremium)
        assertTrue(response.message.contains("Auto-renews monthly"))
    }

    @Test
    fun `test processPurchase prevents duplicate purchases`() {
        val existingSubscription = PremiumSubscription(
            userId = testUserId,
            isPremium = true,
            purchaseToken = testPurchaseToken,
            purchaseDate = LocalDateTime.now().minusDays(1)
        )

        val request = PremiumPurchaseRequest(
            purchaseToken = testPurchaseToken,
            purchasePlatform = "GOOGLE_PLAY"
        )

        every { premiumSubscriptionRepository.findByPurchaseToken(testPurchaseToken) } returns existingSubscription

        val response = premiumService.processPurchase(testUserId, request)

        assertFalse(response.success)
        assertTrue(response.message.contains("already been processed"))
        verify(exactly = 0) { premiumSubscriptionRepository.save(any()) }
    }

    @Test
    fun `test getPremiumStatus returns active premium status`() {
        val now = LocalDateTime.now()
        val subscription = PremiumSubscription(
            userId = testUserId,
            isPremium = true,
            purchaseDate = now.minusDays(10),
            expiryDate = now.plusDays(20),
            purchasePlatform = "GOOGLE_PLAY",
            autoRenew = true
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns subscription

        val response = premiumService.getPremiumStatus(testUserId)

        assertTrue(response.isPremium)
        assertNotNull(response.purchaseDate)
        assertNotNull(response.expiryDate)
        assertEquals(20, response.daysRemaining)
        assertEquals("GOOGLE_PLAY", response.purchasePlatform)
        assertTrue(response.autoRenew)
    }

    @Test
    fun `test getPremiumStatus returns non-premium status when no subscription`() {
        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns null

        val response = premiumService.getPremiumStatus(testUserId)

        assertFalse(response.isPremium)
        assertNull(response.purchaseDate)
        assertNull(response.expiryDate)
        assertNull(response.daysRemaining)
        assertFalse(response.autoRenew)
    }

    @Test
    fun `test getPremiumStatus updates expired subscription`() {
        val now = LocalDateTime.now()
        val expiredSubscription = PremiumSubscription(
            userId = testUserId,
            isPremium = true,
            purchaseDate = now.minusDays(60),
            expiryDate = now.minusDays(1),
            autoRenew = false
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns expiredSubscription
        every { premiumSubscriptionRepository.save(any()) } answers { firstArg() }

        val response = premiumService.getPremiumStatus(testUserId)

        assertFalse(response.isPremium)
        assertEquals(0, response.daysRemaining)
        verify { premiumSubscriptionRepository.save(match { !it.isPremium && !it.autoRenew }) }
    }

    @Test
    fun `test restorePurchase succeeds for valid token`() {
        val now = LocalDateTime.now()
        val subscription = PremiumSubscription(
            userId = testUserId,
            isPremium = true,
            purchaseToken = testPurchaseToken,
            purchaseDate = now.minusDays(5),
            expiryDate = now.plusYears(99)
        )

        every { premiumSubscriptionRepository.findByPurchaseToken(testPurchaseToken) } returns subscription

        val response = premiumService.restorePurchase(testUserId, testPurchaseToken)

        assertTrue(response.success)
        assertTrue(response.isPremium)
        assertTrue(response.message.contains("confirmed"))
    }

    @Test
    fun `test restorePurchase fails for non-existent token`() {
        every { premiumSubscriptionRepository.findByPurchaseToken(testPurchaseToken) } returns null

        val response = premiumService.restorePurchase(testUserId, testPurchaseToken)

        assertFalse(response.success)
        assertFalse(response.isPremium)
        assertTrue(response.message.contains("No purchase found"))
    }

    @Test
    fun `test restorePurchase fails for expired subscription`() {
        val now = LocalDateTime.now()
        val expiredSubscription = PremiumSubscription(
            userId = UUID.randomUUID(),
            isPremium = true,
            purchaseToken = testPurchaseToken,
            purchaseDate = now.minusDays(60),
            expiryDate = now.minusDays(1)
        )

        every { premiumSubscriptionRepository.findByPurchaseToken(testPurchaseToken) } returns expiredSubscription

        val response = premiumService.restorePurchase(testUserId, testPurchaseToken)

        assertFalse(response.success)
        assertTrue(response.message.contains("expired"))
    }

    @Test
    fun `test restorePurchase transfers purchase to new user`() {
        val oldUserId = UUID.randomUUID()
        val now = LocalDateTime.now()
        val subscription = PremiumSubscription(
            userId = oldUserId,
            isPremium = true,
            purchaseToken = testPurchaseToken,
            purchaseDate = now.minusDays(5),
            expiryDate = now.plusYears(99)
        )

        val newUser = User(
            id = testUserId,
            username = "newuser",
            email = "new@example.com",
            passwordHash = "hashedpassword",
            isPremium = false
        )

        every { premiumSubscriptionRepository.findByPurchaseToken(testPurchaseToken) } returns subscription
        every { premiumSubscriptionRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById(testUserId) } returns Optional.of(newUser)
        every { userRepository.save(any()) } answers { firstArg() }

        val response = premiumService.restorePurchase(testUserId, testPurchaseToken)

        assertTrue(response.success)
        assertTrue(response.isPremium)
        assertTrue(response.message.contains("new device"))
        verify { premiumSubscriptionRepository.save(match { it.userId == testUserId }) }
        verify { userRepository.save(match { it.isPremium }) }
    }

    @Test
    fun `test cancelAutoRenewal succeeds for subscription with auto-renew`() {
        val now = LocalDateTime.now()
        val subscription = PremiumSubscription(
            userId = testUserId,
            isPremium = true,
            autoRenew = true,
            expiryDate = now.plusDays(25)
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns subscription
        every { premiumSubscriptionRepository.save(any()) } answers { firstArg() }

        val result = premiumService.cancelAutoRenewal(testUserId)

        assertTrue(result["success"] as Boolean)
        assertTrue((result["message"] as String).contains("cancelled"))
        verify { premiumSubscriptionRepository.save(match { !it.autoRenew }) }
    }

    @Test
    fun `test cancelAutoRenewal fails when auto-renew already disabled`() {
        val subscription = PremiumSubscription(
            userId = testUserId,
            isPremium = true,
            autoRenew = false
        )

        every { premiumSubscriptionRepository.findByUserId(testUserId) } returns subscription

        val result = premiumService.cancelAutoRenewal(testUserId)

        assertFalse(result["success"] as Boolean)
        assertTrue((result["message"] as String).contains("already disabled"))
        verify(exactly = 0) { premiumSubscriptionRepository.save(any()) }
    }

    @Test
    fun `test checkExpiredSubscriptions updates expired subscriptions`() {
        val now = LocalDateTime.now()
        val activeSubscription = PremiumSubscription(
            userId = UUID.randomUUID(),
            isPremium = true,
            expiryDate = now.plusDays(10)
        )
        val expiredSubscription = PremiumSubscription(
            userId = UUID.randomUUID(),
            isPremium = true,
            expiryDate = now.minusDays(1)
        )

        every { premiumSubscriptionRepository.findAll() } returns listOf(activeSubscription, expiredSubscription)
        every { premiumSubscriptionRepository.save(any()) } answers { firstArg() }
        every { userRepository.findById(any()) } returns Optional.of(mockk(relaxed = true))
        every { userRepository.save(any()) } answers { firstArg() }

        val result = premiumService.checkExpiredSubscriptions()

        assertEquals(2, result["totalSubscriptions"])
        assertEquals(1, result["expiredSubscriptions"])
        assertEquals(1, result["activeSubscriptions"])
    }

    @Test
    fun `test getPremiumStatistics returns correct stats`() {
        val now = LocalDateTime.now()
        val subscriptions = listOf(
            PremiumSubscription(userId = UUID.randomUUID(), isPremium = true, autoRenew = false, expiryDate = now.plusDays(90), purchasePlatform = "GOOGLE_PLAY"),
            PremiumSubscription(userId = UUID.randomUUID(), isPremium = true, autoRenew = true, expiryDate = now.plusDays(15), purchasePlatform = "GOOGLE_PLAY"),
            PremiumSubscription(userId = UUID.randomUUID(), isPremium = false, autoRenew = false, expiryDate = now.minusDays(5), purchasePlatform = "APP_STORE")
        )

        every { premiumSubscriptionRepository.findAll() } returns subscriptions

        val stats = premiumService.getPremiumStatistics()

        assertEquals(3, stats["totalSubscriptions"])
        assertEquals(2, stats["activePremium"])
        assertEquals(1, stats["lifetimePurchases"])
        assertEquals(1, stats["monthlySubscriptions"])
        assertTrue(stats.containsKey("platformDistribution"))
        assertTrue(stats.containsKey("conversionRate"))
    }
}
