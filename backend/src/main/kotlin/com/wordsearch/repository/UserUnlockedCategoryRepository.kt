package com.wordsearch.repository

import com.wordsearch.model.UserUnlockedCategory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserUnlockedCategoryRepository : JpaRepository<UserUnlockedCategory, UUID> {
    fun existsByUserIdAndCategoryId(userId: UUID, categoryId: UUID): Boolean
    fun findByUserId(userId: UUID): List<UserUnlockedCategory>
}
