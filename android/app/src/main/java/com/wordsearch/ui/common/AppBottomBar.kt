package com.wordsearch.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** The four primary destinations reachable from the persistent bottom navigation bar. */
enum class BottomDest { DAILY, CATEGORIES, ACHIEVEMENTS, LEADERBOARD }

/**
 * Persistent bottom navigation shown on every main screen so the user can jump between
 * Daily, Categories, Achievements and Leaderboard from anywhere. [current] highlights the
 * active tab (or null on screens that aren't one of the four, e.g. Store/League).
 */
@Composable
fun AppBottomBar(current: BottomDest?, onSelect: (BottomDest) -> Unit) {
    data class Tab(val dest: BottomDest, val icon: ImageVector, val label: String)
    val tabs = listOf(
        Tab(BottomDest.DAILY, Icons.Default.Star, "Daily"),
        Tab(BottomDest.CATEGORIES, Icons.Default.GridView, "Categories"),
        Tab(BottomDest.ACHIEVEMENTS, Icons.Default.EmojiEvents, "Achievements"),
        Tab(BottomDest.LEADERBOARD, Icons.Default.Leaderboard, "Leaderboard"),
    )
    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) },
                selected = current == tab.dest,
                onClick = { if (current != tab.dest) onSelect(tab.dest) }
            )
        }
    }
}
