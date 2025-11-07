package com.wordsearch.data.api

import com.wordsearch.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface GameApi {

    // Categories
    @GET("api/categories")
    suspend fun getCategories(): Response<List<Category>>

    @GET("api/categories/{id}")
    suspend fun getCategory(@Path("id") categoryId: String): Response<Category>

    // Game Session
    @POST("api/game/session/start")
    suspend fun startSession(@Body request: StartSessionRequest): Response<GameSession>

    @POST("api/game/session/{id}/submit-word")
    suspend fun submitWord(
        @Path("id") sessionId: String,
        @Body request: SubmitWordRequest
    ): Response<WordSubmissionResponse>

    @POST("api/game/session/{id}/end")
    suspend fun endSession(@Path("id") sessionId: String): Response<SessionEndResponse>

    @GET("api/game/session/active")
    suspend fun getActiveSession(): Response<GameSession>

    @POST("api/game/session/{id}/save")
    suspend fun saveCasualProgress(@Path("id") sessionId: String): Response<SessionSummary>

    @POST("api/game/session/{id}/resume")
    suspend fun resumeCasualGame(@Path("id") sessionId: String): Response<GameSession>

    // User Progress
    @GET("api/user/progress")
    suspend fun getUserProgress(): Response<UserProgress>

    @GET("api/user/progress/statistics")
    suspend fun getStatistics(): Response<UserStatistics>

    @POST("api/user/progress/update-streak")
    suspend fun updateStreak(): Response<StreakResponse>

    @GET("api/user/progress/leaderboard")
    suspend fun getLeaderboard(@Query("limit") limit: Int = 100): Response<List<LeaderboardEntry>>

    // Boss Levels
    @GET("api/boss/is-boss-level")
    suspend fun isBossLevel(@Query("level") level: Int): Response<BossLevelCheckResponse>

    @POST("api/boss/start")
    suspend fun startBossLevel(): Response<BossLevelStartResponse>

    @POST("api/boss/{attemptId}/complete")
    suspend fun completeBossLevel(
        @Path("attemptId") attemptId: String,
        @Body request: CompleteBossRequest
    ): Response<BossCompletionResponse>

    // Achievements
    @GET("api/achievements")
    suspend fun getAchievements(): Response<AchievementSummary>

    @POST("api/achievements/check")
    suspend fun checkAchievements(): Response<List<AchievementUnlocked>>

    @GET("api/achievements/recent")
    suspend fun getRecentAchievements(@Query("limit") limit: Int = 10): Response<List<Achievement>>

    // Daily Challenge
    @GET("api/daily-challenge/today")
    suspend fun getTodaysChallenge(): Response<DailyChallenge>

    @POST("api/daily-challenge/start")
    suspend fun startDailyChallenge(): Response<DailyChallengeStart>

    @POST("api/daily-challenge/{id}/complete")
    suspend fun completeDailyChallenge(
        @Path("id") attemptId: String,
        @Body request: CompleteChallengeRequest
    ): Response<DailyChallengeCompletion>

    @GET("api/daily-challenge/history")
    suspend fun getChallengeHistory(@Query("limit") limit: Int = 30): Response<ChallengeHistory>

    // Ads
    @GET("api/ads/should-show")
    suspend fun shouldShowAd(): Response<AdShouldShowResponse>

    @POST("api/ads/view")
    suspend fun recordAdView(@Body request: RecordAdRequest): Response<AdViewResponse>

    @GET("api/ads/session-status")
    suspend fun getAdSessionStatus(): Response<AdSessionStatus>

    // Premium
    @GET("api/premium/status")
    suspend fun getPremiumStatus(): Response<PremiumStatus>

    @POST("api/premium/purchase")
    suspend fun processPurchase(@Body request: PurchaseRequest): Response<PurchaseResponse>

    @POST("api/premium/restore")
    suspend fun restorePurchase(@Body request: RestoreRequest): Response<RestoreResponse>
}

// Additional request/response models
data class StartSessionRequest(
    val categoryId: String,
    val gameMode: String = "CLASSIC"
)
data class SessionEndResponse(val finalScore: Int, val wordsFound: Int, val sessionDuration: Int)
data class SessionSummary(
    val sessionId: String,
    val startingLevel: Int,
    val endingLevel: Int,
    val totalScore: Int,
    val wordsFound: Int,
    val highestCombo: Int,
    val duration: Long
)
data class UserStatistics(val totalGamesPlayed: Int, val averageScore: Int, val favoriteCategory: String)
data class StreakResponse(val currentStreak: Int, val message: String)
data class BossLevelCheckResponse(val isBossLevel: Boolean, val bossType: String?)
data class BossLevelStartResponse(val attemptId: String, val bossType: String, val timeLimit: Int)
data class CompleteBossRequest(val wordsFound: Int, val timeTaken: Int)
data class BossCompletionResponse(val completed: Boolean, val reward: Int, val message: String)
data class AchievementSummary(val totalAchievements: Int, val unlocked: Int, val achievements: List<Achievement>)
data class Achievement(val id: String, val name: String, val description: String, val progress: Int, val total: Int, val unlocked: Boolean)
data class AchievementUnlocked(val achievement: Achievement, val pointsAwarded: Int, val message: String)
data class DailyChallenge(val challengeId: String, val category: String, val targetScore: Int, val targetWords: Int, val timeLimit: Int)
data class DailyChallengeStart(val attemptId: String, val challenge: DailyChallenge)
data class CompleteChallengeRequest(val scoreAchieved: Int, val wordsFound: Int, val timeTaken: Int)
data class DailyChallengeCompletion(val completed: Boolean, val bonusPoints: Int, val totalReward: Int, val message: String)
data class ChallengeHistory(val totalAttempts: Int, val completedChallenges: Int, val currentStreak: Int)
data class AdShouldShowResponse(val shouldShowAd: Boolean, val reason: String, val adsRemaining: Int)
data class RecordAdRequest(val adType: String = "INTERSTITIAL")
data class AdViewResponse(val adRecorded: Boolean, val adsRemaining: Int, val adFreeHourStarted: Boolean, val message: String)
data class AdSessionStatus(val adsWatched: Int, val adsRemaining: Int, val isInAdFreeHour: Boolean, val minutesUntilReset: Int?)
data class PremiumStatus(val isPremium: Boolean, val purchaseDate: String?, val expiryDate: String?)
data class PurchaseRequest(val purchaseToken: String, val purchasePlatform: String)
data class PurchaseResponse(val success: Boolean, val isPremium: Boolean, val message: String)
data class RestoreRequest(val purchaseToken: String)
data class RestoreResponse(val success: Boolean, val isPremium: Boolean, val message: String)
