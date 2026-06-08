package com.wordsearch.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.local.TokenManager
import com.wordsearch.data.model.UserProgress
import com.wordsearch.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    val username: String get() = tokenManager.getUsername() ?: "Player"

    private val _progress = MutableStateFlow<UserProgress?>(null)
    val progress: StateFlow<UserProgress?> = _progress.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            gameRepository.getUserProgress().getOrNull()?.let { _progress.value = it }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            tokenManager.clearToken()
            onDone()
        }
    }
}
