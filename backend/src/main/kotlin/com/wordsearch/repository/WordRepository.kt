package com.wordsearch.repository

import com.wordsearch.model.Word
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WordRepository : JpaRepository<Word, UUID> {
    fun findByCategoryId(categoryId: UUID): List<Word>

    fun findByCategoryIdAndDifficultyLevel(categoryId: UUID, difficultyLevel: Int): List<Word>

    @Query("SELECT w FROM Word w WHERE w.categoryId = :categoryId AND w.difficultyLevel <= :maxDifficulty ORDER BY RAND()")
    fun findRandomWordsByCategoryAndDifficulty(
        categoryId: UUID,
        maxDifficulty: Int
    ): List<Word>
}
