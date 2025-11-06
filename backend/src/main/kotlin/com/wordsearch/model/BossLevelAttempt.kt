package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "boss_level_attempts")
data class BossLevelAttempt(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "session_id", columnDefinition = "UUID")
    val sessionId: UUID,

    @Column(name = "boss_level", nullable = false)
    val bossLevel: Int,

    @Column(name = "boss_type", length = 50)
    @Enumerated(EnumType.STRING)
    val bossType: BossType,

    @Column(name = "words_required")
    val wordsRequired: Int,

    @Column(name = "words_found")
    val wordsFound: Int = 0,

    @Column(name = "time_limit_seconds")
    val timeLimitSeconds: Int,

    @Column(name = "time_taken_seconds")
    val timeTakenSeconds: Int? = null,

    @Column(name = "shuffles_occurred")
    val shufflesOccurred: Int = 0,

    @Column(name = "completed")
    val completed: Boolean = false,

    @Column(name = "score_earned")
    val scoreEarned: Int = 0,

    @Column(name = "attempted_at")
    val attemptedAt: LocalDateTime = LocalDateTime.now()
)

enum class BossType {
    SPEED,
    MEGA,
    REVERSE,
    CHAOS
}
