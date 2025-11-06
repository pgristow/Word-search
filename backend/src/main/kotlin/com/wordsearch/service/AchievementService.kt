package com.wordsearch.service

import com.wordsearch.model.Achievement
import com.wordsearch.model.UserAchievement
import com.wordsearch.repository.AchievementRepository
import com.wordsearch.repository.UserAchievementRepository
import com.wordsearch.repository.UserProgressRepository
import com.wordsearch.repository.BossLevelAttemptRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

data class AchievementResponse(
    val achievementId: UUID,
    val name: String,
    val description: String?,
    val category: String,
    val iconUrl: String?,
    val rewardPoints: Int,
    val progress: Int,
    val requirementValue: Int,
    val completed: Boolean,
    val unlockedAt: LocalDateTime?
)

data class AchievementUnlockedResponse(
    val achievement: AchievementResponse,
    val pointsAwarded: Int,
    val message: String
)

data class UserAchievementSummary(
    val totalAchievements: Int,
    val unlockedAchievements: Int,
    val totalPoints: Int,
    val achievements: List<AchievementResponse>
)

@Service
class AchievementService(
    private val achievementRepository: AchievementRepository,
    private val userAchievementRepository: UserAchievementRepository,
    private val userProgressRepository: UserProgressRepository,
    private val bossLevelAttemptRepository: BossLevelAttemptRepository
) {

    /**
     * Check all achievements for a user and unlock any that meet the requirements
     */
    @Transactional
    fun checkAndUnlockAchievements(userId: UUID): List<AchievementUnlockedResponse> {
        val userProgress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalStateException("User progress not found")

        val activeAchievements = achievementRepository.findByIsActiveTrue()
        val newlyUnlocked = mutableListOf<AchievementUnlockedResponse>()

        for (achievement in activeAchievements) {
            val userAchievement = userAchievementRepository.findByUserIdAndAchievementId(
                userId,
                achievement.id
            )

            // Skip if already completed
            if (userAchievement?.completed == true) {
                continue
            }

            val currentProgress = calculateProgress(userId, achievement, userProgress)

            // Check if achievement is newly completed
            if (currentProgress >= achievement.requirementValue) {
                val unlocked = unlockAchievement(userId, achievement, currentProgress)
                newlyUnlocked.add(unlocked)

                // Award points to user progress
                userProgress.totalScore += achievement.rewardPoints
                userProgressRepository.save(userProgress)
            } else {
                // Update progress if not completed
                updateProgress(userId, achievement, currentProgress)
            }
        }

        return newlyUnlocked
    }

    /**
     * Calculate current progress for a specific achievement
     */
    private fun calculateProgress(
        userId: UUID,
        achievement: Achievement,
        userProgress: com.wordsearch.model.UserProgress
    ): Int {
        return when (achievement.requirementType) {
            "WORDS_FOUND" -> userProgress.totalWordsFound
            "TOTAL_SCORE" -> userProgress.totalScore
            "BOSS_DEFEATED" -> bossLevelAttemptRepository.countByUserIdAndCompleted(userId, true)
            "STREAK_DAYS" -> userProgress.currentStreak
            "REVERSED_WORDS" -> userProgress.totalWordsFound / 10 // Estimate, needs tracking
            "HIGHEST_COMBO" -> 0 // Needs separate tracking
            "FAST_COMPLETION" -> 0 // Needs separate tracking
            "CURRENT_LEVEL" -> userProgress.currentLevel
            else -> 0
        }
    }

    /**
     * Unlock an achievement for a user
     */
    @Transactional
    fun unlockAchievement(
        userId: UUID,
        achievement: Achievement,
        progress: Int
    ): AchievementUnlockedResponse {
        val existingAchievement = userAchievementRepository.findByUserIdAndAchievementId(
            userId,
            achievement.id
        )

        val userAchievement = if (existingAchievement != null) {
            existingAchievement.copy(
                progress = progress,
                completed = true,
                unlockedAt = LocalDateTime.now()
            )
        } else {
            UserAchievement(
                userId = userId,
                achievementId = achievement.id,
                progress = progress,
                completed = true,
                unlockedAt = LocalDateTime.now()
            )
        }

        userAchievementRepository.save(userAchievement)

        val achievementResponse = AchievementResponse(
            achievementId = achievement.id,
            name = achievement.name,
            description = achievement.description,
            category = achievement.category,
            iconUrl = achievement.iconUrl,
            rewardPoints = achievement.rewardPoints,
            progress = progress,
            requirementValue = achievement.requirementValue,
            completed = true,
            unlockedAt = userAchievement.unlockedAt
        )

        return AchievementUnlockedResponse(
            achievement = achievementResponse,
            pointsAwarded = achievement.rewardPoints,
            message = "Congratulations! You've unlocked: ${achievement.name}"
        )
    }

    /**
     * Update progress for an achievement without unlocking
     */
    @Transactional
    fun updateProgress(userId: UUID, achievement: Achievement, progress: Int) {
        val existingAchievement = userAchievementRepository.findByUserIdAndAchievementId(
            userId,
            achievement.id
        )

        if (existingAchievement != null) {
            val updated = existingAchievement.copy(progress = progress)
            userAchievementRepository.save(updated)
        } else {
            val newAchievement = UserAchievement(
                userId = userId,
                achievementId = achievement.id,
                progress = progress,
                completed = false
            )
            userAchievementRepository.save(newAchievement)
        }
    }

    /**
     * Get all achievements with user's progress
     */
    fun getUserAchievements(userId: UUID): UserAchievementSummary {
        val allAchievements = achievementRepository.findByIsActiveTrue()
        val userAchievements = userAchievementRepository.findByUserId(userId)
            .associateBy { it.achievementId }

        val userProgress = userProgressRepository.findByUserId(userId)

        val achievementResponses = allAchievements.map { achievement ->
            val userAchievement = userAchievements[achievement.id]
            val progress = if (userProgress != null) {
                calculateProgress(userId, achievement, userProgress)
            } else {
                0
            }

            AchievementResponse(
                achievementId = achievement.id,
                name = achievement.name,
                description = achievement.description,
                category = achievement.category,
                iconUrl = achievement.iconUrl,
                rewardPoints = achievement.rewardPoints,
                progress = userAchievement?.progress ?: progress,
                requirementValue = achievement.requirementValue,
                completed = userAchievement?.completed ?: false,
                unlockedAt = userAchievement?.unlockedAt
            )
        }

        val unlockedCount = achievementResponses.count { it.completed }
        val totalPoints = achievementResponses
            .filter { it.completed }
            .sumOf { it.rewardPoints }

        return UserAchievementSummary(
            totalAchievements = allAchievements.size,
            unlockedAchievements = unlockedCount,
            totalPoints = totalPoints,
            achievements = achievementResponses
        )
    }

    /**
     * Get achievements by category
     */
    fun getAchievementsByCategory(userId: UUID, category: String): List<AchievementResponse> {
        val achievements = achievementRepository.findByCategory(category)
        val userAchievements = userAchievementRepository.findByUserId(userId)
            .associateBy { it.achievementId }

        val userProgress = userProgressRepository.findByUserId(userId)

        return achievements.map { achievement ->
            val userAchievement = userAchievements[achievement.id]
            val progress = if (userProgress != null) {
                calculateProgress(userId, achievement, userProgress)
            } else {
                0
            }

            AchievementResponse(
                achievementId = achievement.id,
                name = achievement.name,
                description = achievement.description,
                category = achievement.category,
                iconUrl = achievement.iconUrl,
                rewardPoints = achievement.rewardPoints,
                progress = userAchievement?.progress ?: progress,
                requirementValue = achievement.requirementValue,
                completed = userAchievement?.completed ?: false,
                unlockedAt = userAchievement?.unlockedAt
            )
        }
    }

    /**
     * Get recently unlocked achievements
     */
    fun getRecentlyUnlocked(userId: UUID, limit: Int = 10): List<AchievementResponse> {
        val userAchievements = userAchievementRepository
            .findByUserIdAndCompleted(userId, true)
            .sortedByDescending { it.unlockedAt }
            .take(limit)

        val achievementIds = userAchievements.map { it.achievementId }
        val achievements = achievementRepository.findAllById(achievementIds)
            .associateBy { it.id }

        return userAchievements.mapNotNull { userAchievement ->
            val achievement = achievements[userAchievement.achievementId] ?: return@mapNotNull null

            AchievementResponse(
                achievementId = achievement.id,
                name = achievement.name,
                description = achievement.description,
                category = achievement.category,
                iconUrl = achievement.iconUrl,
                rewardPoints = achievement.rewardPoints,
                progress = userAchievement.progress,
                requirementValue = achievement.requirementValue,
                completed = true,
                unlockedAt = userAchievement.unlockedAt
            )
        }
    }
}
