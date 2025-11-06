package com.wordsearch.repository

import com.wordsearch.model.GameSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface GameSessionRepository : JpaRepository<GameSession, UUID> {
    fun findByUserIdAndIsActive(userId: UUID, isActive: Boolean = true): GameSession?
    fun findByUserId(userId: UUID): List<GameSession>
}
