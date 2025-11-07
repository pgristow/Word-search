package com.wordsearch.data.repository

import com.wordsearch.data.api.*
import com.wordsearch.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(
    private val gameApi: GameApi
) {
    // Categories
    suspend fun getCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.getCategories()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch categories"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching categories")
            Result.failure(e)
        }
    }

    // Game Session
    suspend fun startSession(categoryId: String): Result<GameSession> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.startSession(StartSessionRequest(categoryId))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to start session"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error starting session")
            Result.failure(e)
        }
    }

    suspend fun submitWord(
        sessionId: String,
        word: String,
        isReversed: Boolean,
        isDiagonal: Boolean,
        timeElapsed: Int
    ): Result<WordSubmissionResponse> = withContext(Dispatchers.IO) {
        try {
            val request = SubmitWordRequest(word, isReversed, isDiagonal, timeElapsed)
            val response = gameApi.submitWord(sessionId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to submit word"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error submitting word")
            Result.failure(e)
        }
    }

    suspend fun endSession(sessionId: String): Result<SessionEndResponse> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.endSession(sessionId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to end session"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error ending session")
            Result.failure(e)
        }
    }

    // User Progress
    suspend fun getUserProgress(): Result<UserProgress> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.getUserProgress()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch progress"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching progress")
            Result.failure(e)
        }
    }

    suspend fun getLeaderboard(limit: Int = 100): Result<List<LeaderboardEntry>> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.getLeaderboard(limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch leaderboard"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching leaderboard")
            Result.failure(e)
        }
    }

    // Achievements
    suspend fun getAchievements(): Result<AchievementSummary> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.getAchievements()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch achievements"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching achievements")
            Result.failure(e)
        }
    }

    suspend fun checkAchievements(): Result<List<AchievementUnlocked>> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.checkAchievements()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to check achievements"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking achievements")
            Result.failure(e)
        }
    }

    // Daily Challenge
    suspend fun getTodaysChallenge(): Result<DailyChallenge> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.getTodaysChallenge()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch daily challenge"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching daily challenge")
            Result.failure(e)
        }
    }

    // Ads
    suspend fun shouldShowAd(): Result<AdShouldShowResponse> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.shouldShowAd()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to check ad status"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking ad status")
            Result.failure(e)
        }
    }

    suspend fun recordAdView(): Result<AdViewResponse> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.recordAdView(RecordAdRequest())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to record ad view"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error recording ad view")
            Result.failure(e)
        }
    }

    // Premium
    suspend fun getPremiumStatus(): Result<PremiumStatus> = withContext(Dispatchers.IO) {
        try {
            val response = gameApi.getPremiumStatus()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch premium status"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching premium status")
            Result.failure(e)
        }
    }
}
