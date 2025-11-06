package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "ad_views")
data class AdView(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "viewed_at")
    val viewedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "ad_type", length = 50)
    val adType: String? = null,

    @Column(name = "ad_unit_id", length = 100)
    val adUnitId: String? = null,

    @Column(name = "session_id", length = 100)
    val sessionId: String? = null
)

@Entity
@Table(name = "ad_session_state")
data class AdSessionState(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "session_start_time")
    val sessionStartTime: LocalDateTime,

    @Column(name = "ads_watched_count")
    val adsWatchedCount: Int = 0,

    @Column(name = "ads_remaining")
    val adsRemaining: Int = 5,

    @Column(name = "session_expires_at")
    val sessionExpiresAt: LocalDateTime,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "premium_subscriptions")
data class PremiumSubscription(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "is_premium")
    val isPremium: Boolean = false,

    @Column(name = "purchase_date")
    val purchaseDate: LocalDateTime? = null,

    @Column(name = "expiry_date")
    val expiryDate: LocalDateTime? = null,

    @Column(name = "purchase_platform", length = 50)
    val purchasePlatform: String? = null,

    @Column(name = "purchase_token", length = 255)
    val purchaseToken: String? = null,

    @Column(name = "auto_renew")
    val autoRenew: Boolean = false,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
