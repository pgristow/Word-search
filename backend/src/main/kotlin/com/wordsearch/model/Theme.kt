package com.wordsearch.model

import jakarta.persistence.*
import java.util.UUID

/** A purchasable cosmetic theme (maps the V6 `themes` table). */
@Entity
@Table(name = "themes")
data class Theme(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", length = 80)
    val name: String,

    @Column(name = "coin_cost")
    val coinCost: Int,

    @Column(name = "asset_key", length = 80)
    val assetKey: String,

    @Column(name = "is_active")
    val isActive: Boolean = true
)
