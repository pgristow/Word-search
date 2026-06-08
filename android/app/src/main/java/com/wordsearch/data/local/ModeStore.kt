package com.wordsearch.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers which play mode the user last chose on the landing page (Casual vs
 * Competitive), so stat screens (leaderboard, etc.) can reflect that mode.
 */
@Singleton
class ModeStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("ws_mode", Context.MODE_PRIVATE)

    var isCasual: Boolean
        get() = prefs.getBoolean("casual", false)
        set(v) = prefs.edit().putBoolean("casual", v).apply()
}
