package com.wordsearch.data.model

import com.google.gson.annotations.SerializedName

// Request models
data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String
)

// Response models
data class AuthResponse(
    @SerializedName("token")
    val token: String,
    @SerializedName("userId")
    val userId: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("isPremium")
    val isPremium: Boolean = false
)

data class User(
    @SerializedName("id")
    val id: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("isPremium")
    val isPremium: Boolean = false,
    @SerializedName("createdAt")
    val createdAt: String
)
