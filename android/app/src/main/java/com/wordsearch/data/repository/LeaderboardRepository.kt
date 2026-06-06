package com.wordsearch.data.repository

import com.wordsearch.data.api.LeaderboardApi
import com.wordsearch.data.model.LeaderboardRowDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeaderboardRepository @Inject constructor(
    private val leaderboardApi: LeaderboardApi
) {

    suspend fun getBoard(
        boardType: String,
        period: String = "ALL_TIME"
    ): Result<List<LeaderboardRowDto>> = withContext(Dispatchers.IO) {
        try {
            val response = leaderboardApi.getBoard(boardType, period)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch leaderboard"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error fetching leaderboard for boardType=$boardType")
            Result.failure(e)
        }
    }
}
