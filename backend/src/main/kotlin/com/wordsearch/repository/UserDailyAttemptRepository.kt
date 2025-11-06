package com.wordsearch.repository

import com.wordsearch.model.UserDailyAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserDailyAttemptRepository : JpaRepository<UserDailyAttempt, UUID> {
    fun findByUserId(userId: UUID): List<UserDailyAttempt>
    fun findByUserIdAndChallengeId(userId: UUID, challengeId: UUID): UserDailyAttempt?
    fun findByUserIdAndCompleted(userId: UUID, completed: Boolean): List<UserDailyAttempt>
    fun countByUserIdAndCompleted(userId: UUID, completed: Boolean): Int
}
