package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "daily_challenges")
data class DailyChallenge(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "challenge_date", unique = true, nullable = false)
    val challengeDate: LocalDate,

    @Column(name = "category_id", columnDefinition = "UUID")
    val categoryId: UUID,

    @Column(name = "difficulty_level")
    val difficultyLevel: Int = 1,

    @Column(name = "target_score")
    val targetScore: Int,

    @Column(name = "target_words")
    val targetWords: Int,

    @Column(name = "time_limit_seconds")
    val timeLimitSeconds: Int,

    @Column(name = "bonus_multiplier")
    val bonusMultiplier: Float = 1.5f,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "achievements")
data class Achievement(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true, length = 100)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(length = 50)
    val category: String,

    @Column(name = "requirement_type", length = 50)
    val requirementType: String,

    @Column(name = "requirement_value")
    val requirementValue: Int,

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    @Column(name = "reward_points")
    val rewardPoints: Int = 100,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "user_achievements")
data class UserAchievement(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "achievement_id", columnDefinition = "UUID")
    val achievementId: UUID,

    @Column(name = "unlocked_at")
    val unlockedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "progress")
    val progress: Int = 0,

    @Column(name = "completed")
    val completed: Boolean = false
)

@Entity
@Table(name = "user_daily_attempts")
data class UserDailyAttempt(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "challenge_id", columnDefinition = "UUID")
    val challengeId: UUID,

    @Column(name = "score_achieved")
    val scoreAchieved: Int = 0,

    @Column(name = "words_found")
    val wordsFound: Int = 0,

    @Column(name = "time_taken_seconds")
    val timeTakenSeconds: Int? = null,

    @Column(name = "completed")
    val completed: Boolean = false,

    @Column(name = "reward_claimed")
    val rewardClaimed: Boolean = false,

    @Column(name = "attempted_at")
    val attemptedAt: LocalDateTime = LocalDateTime.now()
)
