package com.wordsearch.ui.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.api.Achievement
import com.wordsearch.data.api.AchievementSummary
import com.wordsearch.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AchievementsUiState>(AchievementsUiState.Loading)
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    fun loadAchievements() {
        viewModelScope.launch {
            _uiState.value = AchievementsUiState.Loading

            try {
                val result = gameRepository.getAchievements()
                if (result.isSuccess) {
                    val summary = result.getOrNull()!!
                    _uiState.value = AchievementsUiState.Success(summary)
                } else {
                    _uiState.value = AchievementsUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load achievements"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading achievements")
                _uiState.value = AchievementsUiState.Error(
                    e.message ?: "An error occurred"
                )
            }
        }
    }
}

sealed class AchievementsUiState {
    object Loading : AchievementsUiState()
    data class Success(val summary: AchievementSummary) : AchievementsUiState()
    data class Error(val message: String) : AchievementsUiState()
}
