package com.wordsearch.service

import com.wordsearch.model.PremiumSubscription
import com.wordsearch.repository.PremiumSubscriptionRepository
import com.wordsearch.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

data class PremiumPurchaseRequest(
    val purchaseToken: String,
    val purchasePlatform: String,
    val isSubscription: Boolean = false
)

data class PremiumPurchaseResponse(
    val success: Boolean,
    val isPremium: Boolean,
    val purchaseDate: LocalDateTime?,
    val expiryDate: LocalDateTime?,
    val message: String
)

data class PremiumStatusResponse(
    val isPremium: Boolean,
    val purchaseDate: LocalDateTime?,
    val expiryDate: LocalDateTime?,
    val daysRemaining: Int?,
    val purchasePlatform: String?,
    val autoRenew: Boolean
)

data class PremiumRestoreResponse(
    val success: Boolean,
    val isPremium: Boolean,
    val message: String,
    val purchaseDate: LocalDateTime?
)

@Service
class PremiumService(
    private val premiumSubscriptionRepository: PremiumSubscriptionRepository,
    private val userRepository: UserRepository
) {

    /**
     * Process a premium purchase
     */
    @Transactional
    fun processPurchase(userId: UUID, request: PremiumPurchaseRequest): PremiumPurchaseResponse {
        // Verify user exists
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        // Check if purchase token already exists (prevent duplicate purchases)
        val existingPurchase = premiumSubscriptionRepository.findByPurchaseToken(request.purchaseToken)
        if (existingPurchase != null) {
            return PremiumPurchaseResponse(
                success = false,
                isPremium = existingPurchase.isPremium,
                purchaseDate = existingPurchase.purchaseDate,
                expiryDate = existingPurchase.expiryDate,
                message = "This purchase has already been processed"
            )
        }

        // Get or create premium subscription
        val existing = premiumSubscriptionRepository.findByUserId(userId)

        val now = LocalDateTime.now()
        val expiryDate = if (request.isSubscription) {
            // Monthly subscription
            now.plusMonths(1)
        } else {
            // One-time purchase - lifetime (set to 100 years in future)
            now.plusYears(100)
        }

        val subscription = if (existing != null) {
            // Update existing subscription
            existing.copy(
                isPremium = true,
                purchaseDate = now,
                expiryDate = expiryDate,
                purchasePlatform = request.purchasePlatform,
                purchaseToken = request.purchaseToken,
                autoRenew = request.isSubscription,
                updatedAt = now
            )
        } else {
            // Create new subscription
            PremiumSubscription(
                userId = userId,
                isPremium = true,
                purchaseDate = now,
                expiryDate = expiryDate,
                purchasePlatform = request.purchasePlatform,
                purchaseToken = request.purchaseToken,
                autoRenew = request.isSubscription,
                createdAt = now,
                updatedAt = now
            )
        }

        val saved = premiumSubscriptionRepository.save(subscription)

        // Update user's premium status
        val updatedUser = user.copy(isPremium = true)
        userRepository.save(updatedUser)

        val message = if (request.isSubscription) {
            "Premium subscription activated! Auto-renews monthly."
        } else {
            "Premium activated! You now have lifetime access."
        }

        return PremiumPurchaseResponse(
            success = true,
            isPremium = true,
            purchaseDate = saved.purchaseDate,
            expiryDate = saved.expiryDate,
            message = message
        )
    }

    /**
     * Get premium status for a user
     */
    fun getPremiumStatus(userId: UUID): PremiumStatusResponse {
        val subscription = premiumSubscriptionRepository.findByUserId(userId)

        if (subscription == null || !subscription.isPremium) {
            return PremiumStatusResponse(
                isPremium = false,
                purchaseDate = null,
                expiryDate = null,
                daysRemaining = null,
                purchasePlatform = null,
                autoRenew = false
            )
        }

        // Check if subscription is still valid
        val now = LocalDateTime.now()
        val isExpired = subscription.expiryDate?.isBefore(now) ?: false

        if (isExpired) {
            // Subscription expired, update status
            val updated = subscription.copy(
                isPremium = false,
                autoRenew = false,
                updatedAt = now
            )
            premiumSubscriptionRepository.save(updated)

            return PremiumStatusResponse(
                isPremium = false,
                purchaseDate = subscription.purchaseDate,
                expiryDate = subscription.expiryDate,
                daysRemaining = 0,
                purchasePlatform = subscription.purchasePlatform,
                autoRenew = false
            )
        }

        // Calculate days remaining
        val daysRemaining = subscription.expiryDate?.let {
            java.time.Duration.between(now, it).toDays().toInt()
        }

        return PremiumStatusResponse(
            isPremium = true,
            purchaseDate = subscription.purchaseDate,
            expiryDate = subscription.expiryDate,
            daysRemaining = daysRemaining,
            purchasePlatform = subscription.purchasePlatform,
            autoRenew = subscription.autoRenew
        )
    }

    /**
     * Restore previous purchase (for device changes)
     */
    @Transactional
    fun restorePurchase(userId: UUID, purchaseToken: String): PremiumRestoreResponse {
        // Find purchase by token
        val purchase = premiumSubscriptionRepository.findByPurchaseToken(purchaseToken)

        if (purchase == null) {
            return PremiumRestoreResponse(
                success = false,
                isPremium = false,
                message = "No purchase found with this token",
                purchaseDate = null
            )
        }

        // Check if purchase is still valid
        val now = LocalDateTime.now()
        val isExpired = purchase.expiryDate?.isBefore(now) ?: false

        if (isExpired) {
            return PremiumRestoreResponse(
                success = false,
                isPremium = false,
                message = "This purchase has expired",
                purchaseDate = purchase.purchaseDate
            )
        }

        // Check if purchase belongs to a different user
        if (purchase.userId != userId) {
            // Transfer purchase to new user (device change scenario)
            val updated = purchase.copy(
                userId = userId,
                updatedAt = now
            )
            premiumSubscriptionRepository.save(updated)

            // Update user's premium status
            val user = userRepository.findById(userId)
                .orElseThrow { IllegalArgumentException("User not found") }
            val updatedUser = user.copy(isPremium = true)
            userRepository.save(updatedUser)

            return PremiumRestoreResponse(
                success = true,
                isPremium = true,
                message = "Purchase restored successfully on new device",
                purchaseDate = purchase.purchaseDate
            )
        }

        // Purchase already belongs to this user
        return PremiumRestoreResponse(
            success = true,
            isPremium = true,
            message = "Premium status confirmed",
            purchaseDate = purchase.purchaseDate
        )
    }

    /**
     * Cancel auto-renewal for subscription
     */
    @Transactional
    fun cancelAutoRenewal(userId: UUID): Map<String, Any> {
        val subscription = premiumSubscriptionRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("No premium subscription found")

        if (!subscription.autoRenew) {
            return mapOf(
                "success" to false,
                "message" to "Auto-renewal is already disabled"
            )
        }

        val now = LocalDateTime.now()
        val updated = subscription.copy(
            autoRenew = false,
            updatedAt = now
        )
        premiumSubscriptionRepository.save(updated)

        return mapOf(
            "success" to true,
            "message" to "Auto-renewal cancelled. Premium access will expire on ${subscription.expiryDate}",
            "expiryDate" to subscription.expiryDate
        )
    }

    /**
     * Check and update expired subscriptions
     */
    @Transactional
    fun checkExpiredSubscriptions(): Map<String, Int> {
        val allSubscriptions = premiumSubscriptionRepository.findAll()
        val now = LocalDateTime.now()
        var expiredCount = 0

        for (subscription in allSubscriptions) {
            if (subscription.isPremium && subscription.expiryDate != null && subscription.expiryDate.isBefore(now)) {
                val updated = subscription.copy(
                    isPremium = false,
                    autoRenew = false,
                    updatedAt = now
                )
                premiumSubscriptionRepository.save(updated)

                // Update user's premium status
                val user = userRepository.findById(subscription.userId).orElse(null)
                if (user != null) {
                    val updatedUser = user.copy(isPremium = false)
                    userRepository.save(updatedUser)
                }

                expiredCount++
            }
        }

        return mapOf(
            "totalSubscriptions" to allSubscriptions.size,
            "expiredSubscriptions" to expiredCount,
            "activeSubscriptions" to (allSubscriptions.size - expiredCount)
        )
    }

    /**
     * Get premium statistics
     */
    fun getPremiumStatistics(): Map<String, Any> {
        val allSubscriptions = premiumSubscriptionRepository.findAll()
        val now = LocalDateTime.now()

        val totalSubscriptions = allSubscriptions.size
        val activePremium = allSubscriptions.count {
            it.isPremium && (it.expiryDate == null || it.expiryDate.isAfter(now))
        }
        val lifetimePurchases = allSubscriptions.count {
            !it.autoRenew && it.isPremium
        }
        val monthlySubscriptions = allSubscriptions.count {
            it.autoRenew && it.isPremium
        }
        val platformDistribution = allSubscriptions
            .filter { it.isPremium }
            .groupBy { it.purchasePlatform }
            .mapValues { it.value.size }

        return mapOf(
            "totalSubscriptions" to totalSubscriptions,
            "activePremium" to activePremium,
            "lifetimePurchases" to lifetimePurchases,
            "monthlySubscriptions" to monthlySubscriptions,
            "platformDistribution" to platformDistribution,
            "conversionRate" to if (totalSubscriptions > 0) {
                (activePremium.toDouble() / totalSubscriptions * 100).toInt()
            } else {
                0
            }
        )
    }
}
