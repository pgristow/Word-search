package com.wordsearch.data.repository

import com.wordsearch.data.api.AuthApi
import com.wordsearch.data.local.TokenManager
import com.wordsearch.data.model.AuthResponse
import com.wordsearch.data.model.LoginRequest
import com.wordsearch.data.model.RegisterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager
) {
    suspend fun login(username: String, password: String): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    // Save token and user info
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUserInfo(authResponse.userId, authResponse.username)
                    Result.success(authResponse)
                } else {
                    Result.failure(Exception("Login failed: ${response.message()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Login error")
                Result.failure(e)
            }
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String
    ): Result<AuthResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = authApi.register(RegisterRequest(username, email, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    // Save token and user info
                    tokenManager.saveToken(authResponse.token)
                    tokenManager.saveUserInfo(authResponse.userId, authResponse.username)
                    Result.success(authResponse)
                } else {
                    Result.failure(Exception("Registration failed: ${response.message()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Registration error")
                Result.failure(e)
            }
        }
    }

    suspend fun logout() {
        withContext(Dispatchers.IO) {
            tokenManager.clearToken()
        }
    }

    fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn()
    }

    fun getUsername(): String? {
        return tokenManager.getUsername()
    }
}
