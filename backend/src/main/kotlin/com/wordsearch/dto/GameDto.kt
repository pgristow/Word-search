package com.wordsearch.dto

import jakarta.validation.constraints.NotBlank

// Game Session DTOs
data class StartSessionRequest(
    @field:NotBlank(message = "Category ID is required")
    val categoryId: String
)

data class SubmitWordRequest(
    @field:NotBlank(message = "Word is required")
    val word: String,
    val isReversed: Boolean = false,
    val isDiagonal: Boolean = false,
    val timeElapsed: Int = 0
)

data class WordSubmissionResponse(
    val correct: Boolean,
    val score: Int,
    val combo: Int,
    val levelUp: Boolean = false,
    val newLevel: Int? = null,
    val message: String
)

data class SessionEndResponse(
    val finalScore: Int,
    val wordsFound: Int,
    val sessionDuration: Int
)

data class GameSessionResponse(
    val sessionId: String,
    val level: Int,
    val gridSize: Int,
    val category: String,
    val grid: List<List<Char>>,
    val words: List<WordInfo>,
    val targetWordCount: Int,
    val currentScore: Int = 0,
    val wordsFound: Int = 0,
    val currentCombo: Int = 0
)

data class WordInfo(
    val word: String,
    val isReversed: Boolean = false
)

// User Progress DTOs
data class UserProgressResponse(
    val currentLevel: Int,
    val totalScore: Int,
    val totalWordsFound: Int,
    val highestCombo: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val bossLevelsCompleted: Int
)

data class UserStatistics(
    val totalGamesPlayed: Int,
    val averageScore: Int,
    val favoriteCategory: String
)

data class StreakResponse(
    val currentStreak: Int,
    val message: String
)

// Leaderboard DTOs
data class LeaderboardEntry(
    val rank: Int,
    val username: String,
    val totalScore: Int,
    val currentLevel: Int,
    val isPremium: Boolean
)

// Boss Level DTOs
data class BossLevelCheckResponse(
    val isBossLevel: Boolean,
    val bossType: String? = null
)

data class BossLevelStartResponse(
    val attemptId: String,
    val bossType: String,
    val timeLimit: Int,
    val level: Int
)

data class CompleteBossRequest(
    val wordsFound: Int,
    val timeTaken: Int
)

data class BossCompletionResponse(
    val completed: Boolean,
    val reward: Int,
    val message: String
)

// Achievement DTOs
data class AchievementResponse(
    val id: String,
    val name: String,
    val description: String,
    val progress: Int,
    val total: Int,
    val unlocked: Boolean
)

data class AchievementSummary(
    val totalAchievements: Int,
    val unlocked: Int,
    val achievements: List<AchievementResponse>
)

data class AchievementUnlocked(
    val achievement: AchievementResponse,
    val pointsAwarded: Int,
    val message: String
)

// Daily Challenge DTOs
data class DailyChallengeResponse(
    val challengeId: String,
    val category: String,
    val targetScore: Int,
    val targetWords: Int,
    val timeLimit: Int
)

data class DailyChallengeStart(
    val attemptId: String,
    val challenge: DailyChallengeResponse
)

data class CompleteChallengeRequest(
    val scoreAchieved: Int,
    val wordsFound: Int,
    val timeTaken: Int
)

data class DailyChallengeCompletion(
    val completed: Boolean,
    val bonusPoints: Int,
    val totalReward: Int,
    val message: String
)

data class ChallengeHistory(
    val totalAttempts: Int,
    val completedChallenges: Int,
    val currentStreak: Int
)

// Ad DTOs
data class AdShouldShowResponse(
    val shouldShowAd: Boolean,
    val reason: String,
    val adsRemaining: Int
)

data class RecordAdRequest(
    val adType: String = "INTERSTITIAL"
)

data class AdViewResponse(
    val adRecorded: Boolean,
    val adsRemaining: Int,
    val adFreeHourStarted: Boolean,
    val message: String
)

data class AdSessionStatus(
    val adsWatched: Int,
    val adsRemaining: Int,
    val isInAdFreeHour: Boolean,
    val minutesUntilReset: Int?
)

// Premium DTOs
data class PremiumStatus(
    val isPremium: Boolean,
    val purchaseDate: String? = null,
    val expiryDate: String? = null
)

data class PurchaseRequest(
    val purchaseToken: String,
    val purchasePlatform: String
)

data class PurchaseResponse(
    val success: Boolean,
    val isPremium: Boolean,
    val message: String
)

data class RestoreRequest(
    val purchaseToken: String
)

data class RestoreResponse(
    val success: Boolean,
    val isPremium: Boolean,
    val message: String
)
