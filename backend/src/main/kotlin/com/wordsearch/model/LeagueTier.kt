package com.wordsearch.model

import jakarta.persistence.*

@Entity
@Table(name = "league_tiers")
data class LeagueTier(
    @Id
    @Column(name = "id")
    val id: Int,

    @Column(name = "name", length = 40)
    val name: String,

    @Column(name = "tier_order")
    val tierOrder: Int,

    @Column(name = "promote_count")
    val promoteCount: Int,

    @Column(name = "relegate_count")
    val relegateCount: Int,

    @Column(name = "promotion_reward")
    val promotionReward: Int
)
