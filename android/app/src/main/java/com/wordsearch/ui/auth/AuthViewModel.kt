package com.wordsearch.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    authEvents: com.wordsearch.data.local.AuthEvents
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Emits when the saved session was rejected by the server (re-login required). */
    val sessionExpired = authEvents.sessionExpired

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Username and password cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.login(username, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success("Login successful!")
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register(username: String, email: String, password: String, confirmPassword: String) {
        when {
            username.isBlank() -> {
                _uiState.value = AuthUiState.Error("Username cannot be empty")
                return
            }
            email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                _uiState.value = AuthUiState.Error("Please enter a valid email")
                return
            }
            password.isBlank() || password.length < 6 -> {
                _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
                return
            }
            password != confirmPassword -> {
                _uiState.value = AuthUiState.Error("Passwords do not match")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val result = authRepository.register(username, email, password)
            _uiState.value = if (result.isSuccess) {
                AuthUiState.Success("Registration successful!")
            } else {
                AuthUiState.Error(result.exceptionOrNull()?.message ?: "Registration failed")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val message: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
