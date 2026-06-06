package com.wordsearch.repository

import com.wordsearch.model.UserOwnedTheme
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserOwnedThemeRepository : JpaRepository<UserOwnedTheme, UUID> {
    fun existsByUserIdAndThemeId(userId: UUID, themeId: UUID): Boolean
    fun findByUserId(userId: UUID): List<UserOwnedTheme>
}
