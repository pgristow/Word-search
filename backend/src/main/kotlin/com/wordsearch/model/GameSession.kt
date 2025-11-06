package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "game_sessions")
data class GameSession(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "session_start")
    val sessionStart: LocalDateTime = LocalDateTime.now(),

    @Column(name = "session_end")
    val sessionEnd: LocalDateTime? = null,

    @Column(name = "starting_level")
    val startingLevel: Int,

    @Column(name = "ending_level")
    val endingLevel: Int? = null,

    @Column(name = "total_score")
    val totalScore: Int = 0,

    @Column(name = "words_found")
    val wordsFound: Int = 0,

    @Column(name = "highest_combo")
    val highestCombo: Int = 0,

    @Column(name = "boss_levels_completed")
    val bossLevelsCompleted: Int = 0,

    @Column(name = "is_active")
    val isActive: Boolean = true
)
