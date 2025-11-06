package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "categories")
data class Category(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    val name: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null,

    @Column(name = "icon_url")
    val iconUrl: String? = null,

    @Column(name = "unlock_requirement_type", length = 50)
    @Enumerated(EnumType.STRING)
    val unlockRequirementType: UnlockType? = UnlockType.NONE,

    @Column(name = "unlock_requirement_value")
    val unlockRequirementValue: Int? = 0,

    @Column(name = "display_order")
    val displayOrder: Int = 0,

    @Column(name = "is_active")
    val isActive: Boolean = true,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class UnlockType {
    NONE,
    WORDS,
    LEVEL,
    PUZZLES
}
