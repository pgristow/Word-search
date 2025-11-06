package com.wordsearch.repository

import com.wordsearch.model.BossLevelAttempt
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BossLevelAttemptRepository : JpaRepository<BossLevelAttempt, UUID> {
    fun findBySessionId(sessionId: UUID): List<BossLevelAttempt>
    fun findByUserIdAndBossLevel(userId: UUID, bossLevel: Int): List<BossLevelAttempt>
}
