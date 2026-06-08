package com.wordsearch.controller

import com.wordsearch.dto.ErrorResponse
import com.wordsearch.service.LeaderboardService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/leaderboards")
class LeaderboardController(
    private val leaderboardService: LeaderboardService
) {

    @GetMapping("/{boardType}")
    fun getLeaderboard(
        @PathVariable boardType: String,
        @RequestParam(name = "period", defaultValue = "ALL_TIME") period: String
    ): ResponseEntity<Any> {
        return try {
            // Weekly boards are always "this week" — resolve the period server-side.
            val resolvedPeriod = if (boardType == LeaderboardService.BOARD_GLOBAL_WEEKLY ||
                boardType == LeaderboardService.BOARD_CASUAL_WEEKLY) {
                leaderboardService.currentWeekKey()
            } else period
            val entries = leaderboardService.topEntries(boardType, resolvedPeriod)
            val ranked = entries.mapIndexed { index, entry ->
                mapOf(
                    "rank" to index + 1,
                    "userId" to entry.userId.toString(),
                    "username" to entry.username,
                    "score" to entry.score
                )
            }
            // Bare array to match the Android LeaderboardApi contract (List<LeaderboardRowDto>).
            ResponseEntity.ok(ranked)
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Failed to load leaderboard"))
        }
    }
}
