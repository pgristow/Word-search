package com.wordsearch.repository

import com.wordsearch.model.UserProgress
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserProgressRepository : JpaRepository<UserProgress, UUID> {
    fun findByUserId(userId: UUID): UserProgress?
}
