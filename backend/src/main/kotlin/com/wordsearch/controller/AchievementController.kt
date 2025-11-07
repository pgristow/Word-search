package com.wordsearch.controller

import com.wordsearch.service.AchievementService
import com.wordsearch.service.AchievementResponse
import com.wordsearch.service.AchievementUnlockedResponse
import com.wordsearch.service.UserAchievementSummary
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/achievements")
class AchievementController(
    private val achievementService: AchievementService
) {

    /**
     * Get all achievements with user's progress
     * GET /api/achievements
     */
    @GetMapping
    fun getUserAchievements(authentication: Authentication): ResponseEntity<UserAchievementSummary> {
        val userId = UUID.fromString(authentication.principal as String)
        val summary = achievementService.getUserAchievements(userId)
        return ResponseEntity.ok(summary)
    }

    /**
     * Get achievements by category
     * GET /api/achievements/category/{category}
     */
    @GetMapping("/category/{category}")
    fun getAchievementsByCategory(
        @PathVariable category: String,
        authentication: Authentication
    ): ResponseEntity<List<AchievementResponse>> {
        val userId = UUID.fromString(authentication.principal as String)
        val achievements = achievementService.getAchievementsByCategory(userId, category.uppercase())
        return ResponseEntity.ok(achievements)
    }

    /**
     * Check and unlock new achievements
     * POST /api/achievements/check
     */
    @PostMapping("/check")
    fun checkAchievements(authentication: Authentication): ResponseEntity<List<AchievementUnlockedResponse>> {
        val userId = UUID.fromString(authentication.principal as String)
        val newlyUnlocked = achievementService.checkAndUnlockAchievements(userId)
        return ResponseEntity.ok(newlyUnlocked)
    }

    /**
     * Get recently unlocked achievements
     * GET /api/achievements/recent
     */
    @GetMapping("/recent")
    fun getRecentlyUnlocked(
        @RequestParam(defaultValue = "10") limit: Int,
        authentication: Authentication
    ): ResponseEntity<List<AchievementResponse>> {
        val userId = UUID.fromString(authentication.principal as String)
        val recentAchievements = achievementService.getRecentlyUnlocked(userId, limit)
        return ResponseEntity.ok(recentAchievements)
    }

    /**
     * Get achievement summary statistics
     * GET /api/achievements/summary
     */
    @GetMapping("/summary")
    fun getAchievementSummary(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val userId = UUID.fromString(authentication.principal as String)
        val summary = achievementService.getUserAchievements(userId)

        val response = mapOf(
            "totalAchievements" to summary.totalAchievements,
            "unlockedAchievements" to summary.unlockedAchievements,
            "totalPoints" to summary.totalPoints,
            "completionPercentage" to if (summary.totalAchievements > 0) {
                (summary.unlockedAchievements.toDouble() / summary.totalAchievements * 100).toInt()
            } else {
                0
            },
            "categorySummary" to summary.achievements
                .groupBy { it.category }
                .mapValues { (_, achievements) ->
                    mapOf(
                        "total" to achievements.size,
                        "unlocked" to achievements.count { it.completed },
                        "points" to achievements.filter { it.completed }.sumOf { it.rewardPoints }
                    )
                }
        )

        return ResponseEntity.ok(response)
    }
}
