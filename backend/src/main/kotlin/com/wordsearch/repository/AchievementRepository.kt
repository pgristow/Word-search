package com.wordsearch.repository

import com.wordsearch.model.Achievement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AchievementRepository : JpaRepository<Achievement, UUID> {
    fun findByIsActiveTrue(): List<Achievement>
    fun findByCategory(category: String): List<Achievement>
    fun findByName(name: String): Achievement?
}
