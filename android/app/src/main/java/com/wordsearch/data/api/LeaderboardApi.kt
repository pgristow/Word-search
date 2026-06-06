package com.wordsearch.data.api

import com.wordsearch.data.model.LeaderboardRowDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LeaderboardApi {

    @GET("api/leaderboards/{type}")
    suspend fun getBoard(
        @Path("type") type: String,
        @Query("period") period: String = "ALL_TIME"
    ): Response<List<LeaderboardRowDto>>
}
