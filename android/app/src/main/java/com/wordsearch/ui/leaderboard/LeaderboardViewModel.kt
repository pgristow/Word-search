package com.wordsearch.ui.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.local.ModeStore
import com.wordsearch.data.local.TokenManager
import com.wordsearch.data.model.LeaderboardRowDto
import com.wordsearch.data.repository.LeaderboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class LeaderboardTab(val label: String) {
    ALL_TIME("Highest Ever"),
    WEEKLY("This Week")
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardRepository: LeaderboardRepository,
    private val tokenManager: TokenManager,
    private val modeStore: ModeStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeaderboardUiState>(LeaderboardUiState.Loading)
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(LeaderboardTab.ALL_TIME)
    val selectedTab: StateFlow<LeaderboardTab> = _selectedTab.asStateFlow()

    /** Whether the boards shown reflect Casual play (vs Competitive). */
    val isCasual: Boolean get() = modeStore.isCasual

    /** The backend board key for a tab depends on the current play mode. */
    private fun boardTypeFor(tab: LeaderboardTab): String = if (modeStore.isCasual) {
        if (tab == LeaderboardTab.WEEKLY) "CASUAL_WEEKLY" else "CASUAL_BEST"
    } else {
        if (tab == LeaderboardTab.WEEKLY) "GLOBAL_WEEKLY" else "GLOBAL_CLASSIC"
    }

    val currentUserId: String? = tokenManager.getUserId()

    init {
        loadLeaderboard()
    }

    fun selectTab(tab: LeaderboardTab) {
        if (_selectedTab.value == tab) return
        _selectedTab.value = tab
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _uiState.value = LeaderboardUiState.Loading

            try {
                val result = leaderboardRepository.getBoard(boardTypeFor(_selectedTab.value))
                if (result.isSuccess) {
                    val rows = result.getOrNull() ?: emptyList()
                    _uiState.value = LeaderboardUiState.Success(rows, currentUserId)
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
        val rows: List<LeaderboardRowDto>,
        val currentUserId: String?
    ) : LeaderboardUiState()
    data class Error(val message: String) : LeaderboardUiState()
}
