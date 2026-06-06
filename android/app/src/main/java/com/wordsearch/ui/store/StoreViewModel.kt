package com.wordsearch.ui.store

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wordsearch.data.model.StoreResponse
import com.wordsearch.data.repository.EconomyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val economyRepository: EconomyRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoreUiState>(StoreUiState.Loading)
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    // Transient purchase feedback message
    private val _purchaseMessage = MutableStateFlow<String?>(null)
    val purchaseMessage: StateFlow<String?> = _purchaseMessage.asStateFlow()

    init {
        loadStore()
    }

    fun loadStore() {
        viewModelScope.launch {
            _uiState.value = StoreUiState.Loading

            try {
                val result = economyRepository.getStore()
                if (result.isSuccess) {
                    val data = result.getOrNull()!!
                    _uiState.value = StoreUiState.Success(data)
                } else {
                    _uiState.value = StoreUiState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to load store"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading store")
                _uiState.value = StoreUiState.Error(e.message ?: "An error occurred")
            }
        }
    }

    fun purchase(itemType: String, itemId: String) {
        viewModelScope.launch {
            try {
                val result = economyRepository.purchase(itemType, itemId)
                if (result.isSuccess) {
                    val response = result.getOrNull()!!
                    _purchaseMessage.value = response.message
                    // Reload store to reflect updated ownership and coin balance
                    loadStore()
                } else {
                    _purchaseMessage.value = result.exceptionOrNull()?.message ?: "Purchase failed"
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing purchase")
                _purchaseMessage.value = e.message ?: "An error occurred"
            }
        }
    }

    fun clearPurchaseMessage() {
        _purchaseMessage.value = null
    }
}

sealed class StoreUiState {
    object Loading : StoreUiState()
    data class Success(val data: StoreResponse) : StoreUiState()
    data class Error(val message: String) : StoreUiState()
}
