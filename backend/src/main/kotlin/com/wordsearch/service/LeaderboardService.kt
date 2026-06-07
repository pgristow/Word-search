package com.wordsearch.service

import com.wordsearch.model.LeaderboardEntry
import com.wordsearch.repository.LeaderboardEntryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class LeaderboardService(
    private val leaderboardEntryRepository: LeaderboardEntryRepository
) {

    companion object {
        const val BOARD_GLOBAL_CLASSIC = "GLOBAL_CLASSIC"
        const val BOARD_LEAGUE_WEEKLY = "LEAGUE_WEEKLY"
        const val BOARD_CASUAL_BEST = "CASUAL_BEST"
        const val BOARD_CASUAL_WEEKLY = "CASUAL_WEEKLY"
        const val PERIOD_ALL_TIME = "ALL_TIME"
    }

    /** Inserts or updates the leaderboard row for the (user, board, period). */
    @Transactional
    fun upsert(userId: UUID, username: String, boardType: String, periodKey: String, score: Long): LeaderboardEntry {
        val existing = leaderboardEntryRepository
            .findByUserIdAndBoardTypeAndPeriodKey(userId, boardType, periodKey)

        val entry = existing?.copy(
            username = username,
            score = score,
            updatedAt = LocalDateTime.now()
        ) ?: LeaderboardEntry(
            userId = userId,
            username = username,
            boardType = boardType,
            periodKey = periodKey,
            score = score
        )
        return leaderboardEntryRepository.save(entry)
    }

    /** Top entries for a board/period, ranked by score descending. */
    fun topEntries(boardType: String, periodKey: String, limit: Int = 100): List<LeaderboardEntry> =
        leaderboardEntryRepository
            .findByBoardTypeAndPeriodKeyOrderByScoreDesc(boardType, periodKey)
            .take(limit)
}
