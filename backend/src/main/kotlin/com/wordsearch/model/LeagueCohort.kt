package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "league_cohorts")
data class LeagueCohort(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "tier_id")
    val tierId: Int,

    @Column(name = "week_key", length = 10)
    val weekKey: String,

    @Column(name = "status", length = 10)
    val status: String = "OPEN",

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
