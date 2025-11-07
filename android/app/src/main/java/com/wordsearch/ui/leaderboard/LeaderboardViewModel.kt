package com.wordsearch.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.local.TokenManager
import com.wordsearch.data.model.LeaderboardEntry
import com.wordsearch.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val currentUserId = tokenManager.getUserId()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.value = LeaderboardUiState.Loading

            try {
                val result = gameRepository.getLeaderboard(100)
                if (result.isSuccess) {
                    val entries = result.getOrNull() ?: emptyList()
                    _uiState.value = LeaderboardUiState.Success(entries, currentUserId)
                } else {
                    _uiState.value = LeaderboardUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load leaderboard"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading leaderboard")
                _uiState.value = LeaderboardUiState.Error(
                    e.message ?: "An error occurred"
                )
            }
        }
    }
}

sealed class LeaderboardUiState {
    object Loading : LeaderboardUiState()
    data class Success(
        val entries: List<LeaderboardEntry>,
        val currentUserId: String?
    ) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
}
