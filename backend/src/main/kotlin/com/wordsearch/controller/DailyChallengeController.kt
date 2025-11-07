package com.wordsearch.controller

import com.wordsearch.service.DailyChallengeService
import com.wordsearch.service.DailyChallengeResponse
import com.wordsearch.service.DailyChallengeStartResponse
import com.wordsearch.service.DailyChallengeCompletionResponse
import com.wordsearch.service.DailyChallengeHistory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

data class CompleteChallengeRequest(
    val scoreAchieved: Int,
    val wordsFound: Int,
    val timeTakenSeconds: Int
)

@RestController
@RequestMapping("/api/daily-challenge")
class DailyChallengeController(
    private val dailyChallengeService: DailyChallengeService
) {

    /**
     * Get today's daily challenge
     * GET /api/daily-challenge/today
     */
    @GetMapping("/today")
    fun getTodaysChallenge(authentication: Authentication): ResponseEntity<DailyChallengeResponse> {
        val userId = UUID.fromString(authentication.principal as String)
        val challenge = dailyChallengeService.getTodaysChallenge(userId)
        return ResponseEntity.ok(challenge)
    }

    /**
     * Start today's daily challenge
     * POST /api/daily-challenge/start
     */
    @PostMapping("/start")
    fun startDailyChallenge(authentication: Authentication): ResponseEntity<DailyChallengeStartResponse> {
        val userId = UUID.fromString(authentication.principal as String)
        return try {
            val response = dailyChallengeService.startDailyChallenge(userId)
            ResponseEntity.ok(response)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(null)
        }
    }

    /**
     * Complete a daily challenge
     * POST /api/daily-challenge/{attemptId}/complete
     */
    @PostMapping("/{attemptId}/complete")
    fun completeDailyChallenge(
        @PathVariable attemptId: UUID,
        @RequestBody request: CompleteChallengeRequest,
        authentication: Authentication
    ): ResponseEntity<DailyChallengeCompletionResponse> {
        val userId = UUID.fromString(authentication.principal as String)
        return try {
            val response = dailyChallengeService.completeDailyChallenge(
                attemptId = attemptId,
                userId = userId,
                scoreAchieved = request.scoreAchieved,
                wordsFound = request.wordsFound,
                timeTakenSeconds = request.timeTakenSeconds
            )
            ResponseEntity.ok(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(null)
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(null)
        }
    }

    /**
     * Get user's daily challenge history
     * GET /api/daily-challenge/history
     */
    @GetMapping("/history")
    fun getDailyChallengeHistory(
        @RequestParam(defaultValue = "30") limit: Int,
        authentication: Authentication
    ): ResponseEntity<DailyChallengeHistory> {
        val userId = UUID.fromString(authentication.principal as String)
        val history = dailyChallengeService.getDailyChallengeHistory(userId, limit)
        return ResponseEntity.ok(history)
    }

    /**
     * Generate today's daily challenge (admin/system endpoint)
     * POST /api/daily-challenge/generate
     */
    @PostMapping("/generate")
    fun generateTodaysChallenge(): ResponseEntity<Map<String, Any>> {
        val challenge = dailyChallengeService.generateTodaysChallenge()
        val response = mapOf(
            "challengeId" to challenge.id,
            "challengeDate" to challenge.challengeDate,
            "categoryId" to challenge.categoryId,
            "difficultyLevel" to challenge.difficultyLevel,
            "targetScore" to challenge.targetScore,
            "targetWords" to challenge.targetWords,
            "timeLimitSeconds" to challenge.timeLimitSeconds,
            "message" to "Daily challenge generated successfully"
        )
        return ResponseEntity.ok(response)
    }

    /**
     * Get daily challenge statistics
     * GET /api/daily-challenge/stats
     */
    @GetMapping("/stats")
    fun getDailyChallengeStats(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val userId = UUID.fromString(authentication.principal as String)
        val history = dailyChallengeService.getDailyChallengeHistory(userId, 365)

        val stats = mapOf(
            "totalAttempts" to history.totalAttempts,
            "completedChallenges" to history.completedChallenges,
            "completionRate" to if (history.totalAttempts > 0) {
                (history.completedChallenges.toDouble() / history.totalAttempts * 100).toInt()
            } else {
                0
            },
            "totalRewardsEarned" to history.totalRewardsEarned,
            "currentStreak" to history.currentStreak,
            "longestStreak" to history.longestStreak,
            "averageScore" to if (history.totalAttempts > 0) {
                history.recentAttempts
                    .mapNotNull { it.userAttempt?.scoreAchieved }
                    .average()
                    .toInt()
            } else {
                0
            }
        )

        return ResponseEntity.ok(stats)
    }
}
