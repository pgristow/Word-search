package com.wordsearch.repository

import com.wordsearch.model.LeaderboardEntry
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LeaderboardEntryRepository : JpaRepository<LeaderboardEntry, UUID> {
    fun findByBoardTypeAndPeriodKeyOrderByScoreDesc(boardType: String, periodKey: String): List<LeaderboardEntry>
    fun findByUserIdAndBoardTypeAndPeriodKey(
        userId: UUID,
        boardType: String,
        periodKey: String
    ): LeaderboardEntry?
}
