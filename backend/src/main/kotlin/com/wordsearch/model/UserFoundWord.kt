package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "user_found_words")
data class UserFoundWord(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", columnDefinition = "UUID")
    val userId: UUID,

    @Column(name = "session_id", columnDefinition = "UUID")
    val sessionId: UUID,

    @Column(name = "word")
    val word: String,

    @Column(name = "is_reversed")
    val isReversed: Boolean = false,

    @Column(name = "score_earned")
    val scoreEarned: Int,

    @Column(name = "found_at")
    val foundAt: LocalDateTime = LocalDateTime.now()
)
