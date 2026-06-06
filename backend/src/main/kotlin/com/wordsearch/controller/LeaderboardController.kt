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
            val entries = leaderboardService.topEntries(boardType, period)
            val ranked = entries.mapIndexed { index, entry ->
                mapOf(
                    "rank" to index + 1,
                    "userId" to entry.userId.toString(),
                    "username" to entry.username,
                    "score" to entry.score
                )
            }
            ResponseEntity.ok(mapOf("boardType" to boardType, "period" to period, "entries" to ranked))
        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse(e.message ?: "Failed to load leaderboard"))
        }
    }
}
