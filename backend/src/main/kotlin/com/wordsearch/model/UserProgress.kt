package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "user_progress")
data class UserProgress(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID", unique = true)
    val userId: UUID,

    @Column(name = "current_level")
    val currentLevel: Int = 1,

    @Column(name = "total_score")
    val totalScore: Long = 0,

    @Column(name = "current_session_score")
    val currentSessionScore: Int = 0,

    @Column(name = "highest_level_reached")
    val highestLevelReached: Int = 1,

    @Column(name = "highest_combo")
    val highestCombo: Int = 0,

    @Column(name = "total_words_found")
    val totalWordsFound: Int = 0,

    @Column(name = "total_reversed_words_found")
    val totalReversedWordsFound: Int = 0,

    @Column(name = "casual_puzzles_completed")
    val casualPuzzlesCompleted: Int = 0,

    @Column(name = "coins")
    val coins: Long = 0,

    @Column(name = "total_bonus_words_found")
    val totalBonusWordsFound: Int = 0,

    @Column(name = "longest_word_found")
    val longestWordFound: Int = 0,

    @Column(name = "weekly_score")
    val weeklyScore: Long = 0,

    @Column(name = "casual_best_score")
    val casualBestScore: Long = 0,

    @Column(name = "last_played_at")
    val lastPlayedAt: LocalDateTime? = null,

    @Column(name = "current_streak_days")
    val currentStreakDays: Int = 0,

    @Column(name = "longest_streak_days")
    val longestStreakDays: Int = 0,

    @Column(name = "last_streak_date")
    val lastStreakDate: LocalDate? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
