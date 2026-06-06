package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "coin_transactions")
data class CoinTransaction(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "delta")
    val delta: Long,

    @Column(name = "reason", length = 40)
    val reason: String,

    @Column(name = "ref_id", columnDefinition = "UUID")
    val refId: UUID? = null,

    @Column(name = "balance_after")
    val balanceAfter: Long,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
