package com.wordsearch.data.model

import com.google.gson.annotations.SerializedName

data class LeaderboardRowDto(
    @SerializedName("rank")
    val rank: Int = 0,
    @SerializedName("userId")
    val userId: String = "",
    @SerializedName("username")
    val username: String = "",
    @SerializedName("score")
    val score: Long = 0L
)
