package com.wordsearch.ui.league

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.model.LeagueMeResponse
import com.wordsearch.data.repository.LeagueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val leagueRepository: LeagueRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LeagueUiState>(LeagueUiState.Loading)
    val uiState: StateFlow<LeagueUiState> = _uiState.asStateFlow()

    init {
        loadLeague()
    }

    fun loadLeague() {
        viewModelScope.launch {
            _uiState.value = LeagueUiState.Loading

            try {
                val result = leagueRepository.getMyLeague()
                if (result.isSuccess) {
                    val data = result.getOrNull()!!
                    _uiState.value = LeagueUiState.Success(data)
                } else {
                    _uiState.value = LeagueUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load league"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading league")
                _uiState.value = LeagueUiState.Error(
                    e.message ?: "An error occurred"
                )
            }
        }
    }
}

sealed class LeagueUiState {
    object Loading : LeagueUiState()
    data class Success(val data: LeagueMeResponse) : LeagueUiState()
    data class Error(val message: String) : LeagueUiState()
}
