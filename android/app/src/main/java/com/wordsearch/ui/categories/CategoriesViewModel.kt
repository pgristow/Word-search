package com.wordsearch.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.local.TokenManager
import com.wordsearch.data.model.Category
import com.wordsearch.data.model.UserProgress
import com.wordsearch.data.repository.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<CategoriesUiState>(CategoriesUiState.Loading)
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    private val _userProgress = MutableStateFlow<UserProgress?>(null)
    val userProgress: StateFlow<UserProgress?> = _userProgress.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = CategoriesUiState.Loading

            try {
                // Load categories and user progress in parallel
                val categoriesResult = gameRepository.getCategories()
                val progressResult = gameRepository.getUserProgress()

                if (categoriesResult.isSuccess && progressResult.isSuccess) {
                    val categories = categoriesResult.getOrNull() ?: emptyList()
                    val progress = progressResult.getOrNull()

                    _userProgress.value = progress
                    _uiState.value = CategoriesUiState.Success(categories)
                } else {
                    val error = categoriesResult.exceptionOrNull()
                        ?: progressResult.exceptionOrNull()
                        ?: Exception("Failed to load data")
                    _uiState.value = CategoriesUiState.Error(
                        error.message ?: "Failed to load categories"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading categories")
                _uiState.value = CategoriesUiState.Error(
                    e.message ?: "An error occurred"
                )
            }
        }
    }

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            tokenManager.clearToken()
            onLogoutComplete()
        }
    }

    fun getUsername(): String {
        return tokenManager.getUsername() ?: "Player"
    }
}

sealed class CategoriesUiState {
    object Loading : CategoriesUiState()
    data class Success(val categories: List<Category>) : CategoriesUiState()
    data class Error(val message: String) : CategoriesUiState()
}
