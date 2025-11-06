package com.wordsearch.repository

import com.wordsearch.model.PremiumSubscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PremiumSubscriptionRepository : JpaRepository<PremiumSubscription, UUID> {
    fun findByUserId(userId: UUID): PremiumSubscription?
    fun findByPurchaseToken(purchaseToken: String): PremiumSubscription?
}
