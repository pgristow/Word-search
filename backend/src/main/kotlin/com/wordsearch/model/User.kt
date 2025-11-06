package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
data class User(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(unique = true, nullable = false, length = 50)
    val username: String,

    @Column(unique = true, nullable = false, length = 100)
    val email: String,

    @Column(name = "password_hash", nullable = false)
    val passwordHash: String,

    @Column(name = "total_words_found")
    val totalWordsFound: Int = 0,

    @Column(name = "total_puzzles_completed")
    val totalPuzzlesCompleted: Int = 0,

    @Column(name = "current_level")
    val currentLevel: Int = 1,

    @Column(name = "is_premium")
    val isPremium: Boolean = false,

    @Column(name = "premium_expires_at")
    val premiumExpiresAt: LocalDateTime? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
