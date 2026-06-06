package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "leaderboard_entries")
data class LeaderboardEntry(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "username", length = 80)
    val username: String,

    @Column(name = "board_type", length = 20)
    val boardType: String,

    @Column(name = "period_key", length = 40)
    val periodKey: String,

    @Column(name = "score")
    val score: Long = 0,

    @Column(name = "updated_at")
    val updatedAt: LocalDateTime = LocalDateTime.now()
)
