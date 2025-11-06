package com.wordsearch.repository

import com.wordsearch.model.UserAchievement
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserAchievementRepository : JpaRepository<UserAchievement, UUID> {
    fun findByUserId(userId: UUID): List<UserAchievement>
    fun findByUserIdAndAchievementId(userId: UUID, achievementId: UUID): UserAchievement?
    fun findByUserIdAndCompleted(userId: UUID, completed: Boolean): List<UserAchievement>
    fun countByUserIdAndCompleted(userId: UUID, completed: Boolean): Int
}
