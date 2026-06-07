package com.wordsearch.dto

import jakarta.validation.constraints.NotBlank

// Game Session DTOs
data class StartSessionRequest(
    @field:NotBlank(message = "Category ID is required")
    val categoryId: String,
    val gameMode: String = "CLASSIC" // CLASSIC or CASUAL
)

data class CellDto(val row: Int = 0, val col: Int = 0)

data class SubmitWordRequest(
    @field:NotBlank(message = "Word is required")
    val word: String,
    val isReversed: Boolean = false,
    val isDiagonal: Boolean = false,
    val timeElapsed: Int = 0,
    // Ordered cells the player traced. Required for bonus-word recognition + anti-spoof;
    // empty list falls back to legacy word-only matching (target words only).
    val path: List<CellDto> = emptyList()
)

data class WordSubmissionResponse(
    val correct: Boolean,
    val score: Int,
    val totalScore: Long = 0,
    // Authoritative running score for THIS session (sum of words found this game). The
    // client hard-sets its scoreboard to this so the displayed score can never drift.
    val sessionScore: Int = 0,
    val currentCombo: Int = 0,
    val combo: Int = 0, // Deprecated, use currentCombo
    val levelUp: Boolean = false,
    val leveledUp: Boolean = false, // Deprecated, use levelUp
    val newLevel: Int? = null,
    val wordsFoundInSession: Int = 0,
    val message: String,
    val isBonus: Boolean = false,
    val wordLength: Int = 0,
    val coinsEarned: Long = 0,
    val coinBalance: Long = 0,
    val scoreBreakdown: Map<String, Int> = emptyMap()
)

// Hint: reveals one unfound target word (its cells) in exchange for coins.
data class HintResponse(
    val word: String,
    val cells: List<CellDto>,
    val coinsSpent: Long,
    val coinBalance: Long
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
    val currentCombo: Int = 0,
    val gameMode: String = "CLASSIC", // CLASSIC or CASUAL
    val foundWords: List<String> = emptyList() // List of words already found in this session
)

data class WordInfo(
    val word: String,
    val isReversed: Boolean = false,
    val startRow: Int = 0,
    val startCol: Int = 0,
    val direction: String = ""
)

// User Progress DTOs
data class UserProgressResponse(
    val currentLevel: Int,
    val totalScore: Int,
    val totalWordsFound: Int,
    val highestCombo: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val bossLevelsCompleted: Int,
    val casualPuzzlesCompleted: Int = 0
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

// Store purchase: itemType is "THEME" or "CATEGORY"; itemId is the target UUID.
data class PurchaseItemRequest(
    @field:NotBlank(message = "itemType is required")
    val itemType: String,
    @field:NotBlank(message = "itemId is required")
    val itemId: String
)

data class RestoreResponse(
    val success: Boolean,
    val isPremium: Boolean,
    val message: String
)
