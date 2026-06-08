package com.wordsearch.data.local

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide channel for auth events. When a request is rejected because the saved token is
 * expired/invalid, the network layer clears it and emits here so the UI can bounce the
 * user back to the login screen instead of getting stuck on a generic error.
 */
@Singleton
class AuthEvents @Inject constructor() {
    private val _sessionExpired = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()
    fun notifySessionExpired() { _sessionExpired.tryEmit(Unit) }
}
