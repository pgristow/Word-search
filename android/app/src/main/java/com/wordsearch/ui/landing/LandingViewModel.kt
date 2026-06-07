package com.wordsearch.ui.landing

import androidx.lifecycle.ViewModel
import com.wordsearch.data.local.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val tokenManager: TokenManager
) : ViewModel() {
    val username: String get() = tokenManager.getUsername() ?: "Player"
}
