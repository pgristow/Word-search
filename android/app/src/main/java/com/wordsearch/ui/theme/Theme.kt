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

@Composable
fun WordSearchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = CalmColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
