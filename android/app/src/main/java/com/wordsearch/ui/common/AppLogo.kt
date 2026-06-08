@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.wordsearch.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wordsearch.R

// Rounded, bubbly display face (Fredoka, OFL) bundled in res/font.
private val FredokaFamily = FontFamily(
    Font(R.font.fredoka, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

// Playful logo palette.
private val LogoPink = Color(0xFFFF5E8A)
private val LogoYellow = Color(0xFFFFC83D)
private val LogoOutline = Color(0xFF2B2150)
private val LogoBgTop = Color(0xFF6A5AE0)
private val LogoBgBottom = Color(0xFF8E7BEF)

/**
 * The app logo: the brand name in big, stacked, bubble-style letters with a thick dark
 * outline so it reads like a sticker/cartoon wordmark. Alternating line colours add pop.
 */
@Composable
fun AppLogo(modifier: Modifier = Modifier, fontSize: TextUnit = 64.sp) {
    val lines = AppBranding.logoLines
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        // Stacked lines overlap slightly so the wordmark reads as one bubbly logo.
        verticalArrangement = Arrangement.spacedBy((-14).dp)
    ) {
        lines.forEachIndexed { i, line ->
            BubbleText(
                text = line,
                fontSize = fontSize,
                fill = if (i % 2 == 0) LogoYellow else LogoPink,
                outline = LogoOutline
            )
        }
    }
}

/**
 * One line of bubble text in the rounded Fredoka face: a soft drop shadow, a thick dark
 * outline (8 offset copies), the colour fill, then a top sheen — giving each letter a
 * glossy balloon look while staying clearly legible.
 */
@Composable
private fun BubbleText(text: String, fontSize: TextUnit, fill: Color, outline: Color) {
    Box(contentAlignment = Alignment.Center) {
        // Drop shadow under the whole word.
        Text(
            text = text, fontSize = fontSize, fontWeight = FontWeight.Bold,
            fontFamily = FredokaFamily, color = Color.Black.copy(alpha = 0.22f),
            modifier = Modifier.offset(0.dp, 5.dp)
        )
        // Thick rounded outline.
        val o = 4
        listOf(
            -o to 0, o to 0, 0 to -o, 0 to o,
            -o to -o, o to o, -o to o, o to -o
        ).forEach { (dx, dy) ->
            Text(
                text = text, fontSize = fontSize, fontWeight = FontWeight.Bold,
                fontFamily = FredokaFamily, color = outline,
                modifier = Modifier.offset(dx.dp, dy.dp)
            )
        }
        // Colour fill.
        Text(
            text = text, fontSize = fontSize, fontWeight = FontWeight.Bold,
            fontFamily = FredokaFamily, color = fill
        )
        // Top sheen — the balloon highlight.
        Text(
            text = text, fontSize = fontSize, fontWeight = FontWeight.Bold,
            fontFamily = FredokaFamily, color = Color.White.copy(alpha = 0.28f),
            modifier = Modifier.offset(0.dp, (-3).dp)
        )
    }
}

/**
 * Branded full-screen loading view shown before a game board arrives — the WordPop logo on
 * a soft gradient with a spinner. Replaces the old plain/booky loading background.
 */
@Composable
fun BrandedLoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LogoBgTop, LogoBgBottom))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogo()
            Spacer(Modifier.height(28.dp))
            Text(
                "Loading your puzzle…",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator(color = Color.White)
        }
    }
}
