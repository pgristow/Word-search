package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/** A category a user has unlocked (maps the V6 `user_unlocked_categories` table). */
@Entity
@Table(name = "user_unlocked_categories")
data class UserUnlockedCategory(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "category_id", columnDefinition = "UUID")
    val categoryId: UUID,

    /** How it was unlocked, e.g. "COINS". */
    @Column(name = "method", length = 20)
    val method: String,

    @Column(name = "unlocked_at")
    val unlockedAt: LocalDateTime = LocalDateTime.now()
)
