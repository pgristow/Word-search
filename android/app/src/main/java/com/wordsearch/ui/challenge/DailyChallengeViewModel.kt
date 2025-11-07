package com.wordsearch.ui.challenge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.api.ChallengeHistory
import com.wordsearch.data.api.DailyChallenge
import com.wordsearch.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DailyChallengeViewModel @Inject constructor(
    private val gameRepository: GameRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DailyChallengeUiState>(DailyChallengeUiState.Loading)
    val uiState: StateFlow<DailyChallengeUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<ChallengeHistory?>(null)
    val history: StateFlow<ChallengeHistory?> = _history.asStateFlow()

    init {
        loadDailyChallenge()
    }

    fun loadDailyChallenge() {
        viewModelScope.launch {
            _uiState.value = DailyChallengeUiState.Loading

            try {
                // Load today's challenge and history in parallel
                val challengeResult = gameRepository.getTodaysChallenge()
                val historyResult = gameRepository.getChallengeHistory(30)

                if (challengeResult.isSuccess) {
                    val challenge = challengeResult.getOrNull()!!
                    _uiState.value = DailyChallengeUiState.Available(challenge)

                    // Set history if available
                    historyResult.getOrNull()?.let { hist ->
                        _history.value = hist
                    }
                } else {
                    _uiState.value = DailyChallengeUiState.Error(
                        challengeResult.exceptionOrNull()?.message ?: "Failed to load daily challenge"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading daily challenge")
                _uiState.value = DailyChallengeUiState.Error(
                    e.message ?: "An error occurred"
                )
            }
        }
    }

    fun startChallenge(onSuccess: (String, String) -> Unit) {
        val currentState = _uiState.value
        if (currentState !is DailyChallengeUiState.Available) return

        viewModelScope.launch {
            try {
                val result = gameRepository.startDailyChallenge()
                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    // Navigate to game screen with challenge attempt ID
                    onSuccess(response.attemptId, response.challenge.challengeId)
                } else {
                    _uiState.value = DailyChallengeUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to start challenge"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting challenge")
                _uiState.value = DailyChallengeUiState.Error(
                    e.message ?: "An error occurred"
                )
            }
        }
    }
}

sealed class DailyChallengeUiState {
    object Loading : DailyChallengeUiState()
    data class Available(val challenge: DailyChallenge) : DailyChallengeUiState()
    data class Error(val message: String) : DailyChallengeUiState()
}
