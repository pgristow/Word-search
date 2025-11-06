package com.wordsearch.repository

import com.wordsearch.model.AdView
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface AdViewRepository : JpaRepository<AdView, UUID> {
    fun findByUserId(userId: UUID): List<AdView>
    fun findByUserIdAndViewedAtAfter(userId: UUID, viewedAt: LocalDateTime): List<AdView>
    fun countByUserIdAndViewedAtAfter(userId: UUID, viewedAt: LocalDateTime): Int
}
