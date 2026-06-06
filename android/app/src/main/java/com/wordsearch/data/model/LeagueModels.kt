package com.wordsearch.data.model

import com.google.gson.annotations.SerializedName

data class LeagueStandingDto(
    @SerializedName("rank")
    val rank: Int = 0,
    @SerializedName("userId")
    val userId: String = "",
    @SerializedName("username")
    val username: String = "",
    @SerializedName("weeklyScore")
    val weeklyScore: Long = 0L,
    @SerializedName("result")
    val result: String? = null
)

data class LeagueMeResponse(
    @SerializedName("tierName")
    val tierName: String = "",
    @SerializedName("tierOrder")
    val tierOrder: Int = 1,
    @SerializedName("weekKey")
    val weekKey: String = "",
    @SerializedName("myUserId")
    val myUserId: String = "",
    @SerializedName("standings")
    val standings: List<LeagueStandingDto> = emptyList()
)
