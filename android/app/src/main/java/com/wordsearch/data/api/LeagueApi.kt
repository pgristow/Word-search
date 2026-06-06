package com.wordsearch.data.api

import com.wordsearch.data.model.LeagueMeResponse
import retrofit2.Response
import retrofit2.http.GET

interface LeagueApi {

    @GET("api/leagues/me")
    suspend fun getMyLeague(): Response<LeagueMeResponse>
}
