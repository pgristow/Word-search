package com.wordsearch.service

import com.wordsearch.model.UserProgress
import com.wordsearch.repository.GameSessionRepository
import com.wordsearch.repository.UserProgressRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class UserProgressService(
    private val userProgressRepository: UserProgressRepository,
    private val gameSessionRepository: GameSessionRepository
) {

    fun getUserProgress(userId: UUID): UserProgressResponse {
        val progress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("User progress not found")

        return UserProgressResponse(
            userId = userId.toString(),
            currentLevel = progress.currentLevel,
            totalScore = progress.totalScore,
            highestLevelReached = progress.highestLevelReached,
            highestCombo = progress.highestCombo,
            totalWordsFound = progress.totalWordsFound,
            totalReversedWordsFound = progress.totalReversedWordsFound,
            currentStreakDays = progress.currentStreakDays,
            longestStreakDays = progress.longestStreakDays,
            lastPlayedAt = progress.lastPlayedAt?.toString()
        )
    }

    fun getUserStatistics(userId: UUID): UserStatistics {
        val progress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("User progress not found")

        val allSessions = gameSessionRepository.findByUserId(userId)
        val completedSessions = allSessions.filter { !it.isActive }

        val totalSessionsPlayed = completedSessions.size
        val averageScorePerSession = if (totalSessionsPlayed > 0) {
            completedSessions.map { it.totalScore }.average()
        } else 0.0

        val averageWordsPerSession = if (totalSessionsPlayed > 0) {
            completedSessions.map { it.wordsFound }.average()
        } else 0.0

        val totalBossLevelsCompleted = completedSessions.sumOf { it.bossLevelsCompleted }

        return UserStatistics(
            totalScore = progress.totalScore,
            currentLevel = progress.currentLevel,
            highestLevelReached = progress.highestLevelReached,
            totalWordsFound = progress.totalWordsFound,
            totalReversedWordsFound = progress.totalReversedWordsFound,
            reversedWordPercentage = if (progress.totalWordsFound > 0) {
                (progress.totalReversedWordsFound.toDouble() / progress.totalWordsFound * 100)
            } else 0.0,
            highestCombo = progress.highestCombo,
            totalSessionsPlayed = totalSessionsPlayed,
            averageScorePerSession = averageScorePerSession.toInt(),
            averageWordsPerSession = averageWordsPerSession.toInt(),
            currentStreakDays = progress.currentStreakDays,
            longestStreakDays = progress.longestStreakDays,
            totalBossLevelsCompleted = totalBossLevelsCompleted
        )
    }

    fun updateStreak(userId: UUID) {
        val progress = userProgressRepository.findByUserId(userId)
            ?: throw IllegalArgumentException("User progress not found")

        val today = LocalDate.now()
        val lastPlayedDate = progress.lastStreakDate

        val updatedProgress = when {
            lastPlayedDate == null -> {
                // First time playing
                progress.copy(
                    currentStreakDays = 1,
                    longestStreakDays = 1,
                    lastStreakDate = today
                )
            }
            lastPlayedDate == today -> {
                // Already played today, no update needed
                progress
            }
            lastPlayedDate == today.minusDays(1) -> {
                // Played yesterday, continue streak
                val newStreak = progress.currentStreakDays + 1
                progress.copy(
                    currentStreakDays = newStreak,
                    longestStreakDays = maxOf(progress.longestStreakDays, newStreak),
                    lastStreakDate = today
                )
            }
            else -> {
                // Streak broken, start over
                progress.copy(
                    currentStreakDays = 1,
                    lastStreakDate = today
                )
            }
        }

        userProgressRepository.save(updatedProgress)
    }

    fun getLeaderboard(limit: Int = 100): List<LeaderboardEntry> {
        val allProgress = userProgressRepository.findAll()

        return allProgress
            .sortedByDescending { it.totalScore }
            .take(limit)
            .mapIndexed { index, progress ->
                LeaderboardEntry(
                    rank = index + 1,
                    userId = progress.userId.toString(),
                    totalScore = progress.totalScore,
                    currentLevel = progress.currentLevel,
                    highestLevelReached = progress.highestLevelReached
                )
            }
    }
}

// DTOs
data class UserProgressResponse(
    val userId: String,
    val currentLevel: Int,
    val totalScore: Long,
    val highestLevelReached: Int,
    val highestCombo: Int,
    val totalWordsFound: Int,
    val totalReversedWordsFound: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val lastPlayedAt: String?
)

data class UserStatistics(
    val totalScore: Long,
    val currentLevel: Int,
    val highestLevelReached: Int,
    val totalWordsFound: Int,
    val totalReversedWordsFound: Int,
    val reversedWordPercentage: Double,
    val highestCombo: Int,
    val totalSessionsPlayed: Int,
    val averageScorePerSession: Int,
    val averageWordsPerSession: Int,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val totalBossLevelsCompleted: Int
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val totalScore: Long,
    val currentLevel: Int,
    val highestLevelReached: Int
)
