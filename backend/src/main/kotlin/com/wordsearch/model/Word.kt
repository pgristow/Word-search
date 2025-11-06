package com.wordsearch.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "words")
data class Word(
    @Id
    @Column(name = "id", columnDefinition = "UUID")
    val id: UUID = UUID.randomUUID(),

    @Column(name = "category_id", columnDefinition = "UUID")
    val categoryId: UUID,

    @Column(nullable = false, length = 50)
    val word: String,

    @Column(name = "difficulty_level")
    val difficultyLevel: Int = 1,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)
