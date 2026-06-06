package com.wordsearch.model

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "league_memberships")
data class LeagueMembership(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "cohort_id", columnDefinition = "UUID")
    val cohortId: UUID,

    @Column(name = "weekly_score")
    val weeklyScore: Long = 0,

    @Column(name = "final_rank")
    val finalRank: Int? = null,

    @Column(name = "result", length = 12)
    val result: String? = null
)
