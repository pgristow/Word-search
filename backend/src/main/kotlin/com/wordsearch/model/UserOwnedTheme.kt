package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/** A theme owned by a user (maps the V6 `user_owned_themes` table). */
@Entity
@Table(name = "user_owned_themes")
data class UserOwnedTheme(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "theme_id", columnDefinition = "UUID")
    val themeId: UUID,

    @Column(name = "acquired_at")
    val acquiredAt: LocalDateTime = LocalDateTime.now()
)
