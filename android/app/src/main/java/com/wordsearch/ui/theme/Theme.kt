package com.wordsearch.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Calm Modern scheme — used regardless of system dark mode so the game's designed
// palette stays consistent.
private val CalmColorScheme = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = BrandIndigoContainer,
    onPrimaryContainer = Ink,
    secondary = FoundTeal,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Ink,
    tertiary = BonusAmber,
    onTertiary = Color.White,
    tertiaryContainer = BonusAmberContainer,
    onTertiaryContainer = Ink,
    background = AppBackground,
    onBackground = Ink,
    surface = AppSurface,
    onSurface = Ink,
    surfaceVariant = BoardTrough,
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFCFC8BA),
    error = DangerRed,
    onError = Color.White,
)

// Calm Modern — dark variant. Follows the system dark-mode setting.
private val CalmDarkScheme = darkColorScheme(
    primary = Color(0xFF9AB0FF),
    onPrimary = Color(0xFF1A2240),
    primaryContainer = Color(0xFF33406E),
    onPrimaryContainer = Color(0xFFDDE2F7),
    secondary = Color(0xFF5FD0B6),
    onSecondary = Color(0xFF06281F),
    secondaryContainer = Color(0xFF1E4A40),
    onSecondaryContainer = Color(0xFFCDEFE7),
    tertiary = Color(0xFFF5B968),
    onTertiary = Color(0xFF3A2400),
    tertiaryContainer = Color(0xFF5A3D14),
    onTertiaryContainer = Color(0xFFFCE6CC),
    background = Color(0xFF14161F),     // deep navy backdrop
    onBackground = Color(0xFFE7E8EE),
    surface = Color(0xFF252A38),        // letter tile
    onSurface = Color(0xFFEDEFF5),      // letters
    surfaceVariant = Color(0xFF0E0F16), // board trough (darker than tile)
    onSurfaceVariant = Color(0xFFB9BCC8),
    outline = Color(0xFF3A3F4D),
    error = Color(0xFFE9837C),
    onError = Color(0xFF3A0E0C),
)

@Composable
fun WordSearchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CalmDarkScheme else CalmColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
