package com.wordsearch.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

/** Per-category theming: a kid-friendly emoji motif and a two-stop background gradient. */
data class CategoryTheme(val emoji: String, val top: Color, val bottom: Color)

fun categoryTheme(name: String): CategoryTheme = when (name.trim().lowercase()) {
    "animals" -> CategoryTheme("🦁", Color(0xFFFFE7C2), Color(0xFFFFB74D))
    "food" -> CategoryTheme("🍔", Color(0xFFFFE3CC), Color(0xFFFF8A65))
    "sports" -> CategoryTheme("⚽", Color(0xFFCDEBCF), Color(0xFF66BB6A))
    "science" -> CategoryTheme("🔬", Color(0xFFE7D3F0), Color(0xFF9575CD))
    "nature" -> CategoryTheme("🌿", Color(0xFFBDE0DB), Color(0xFF4DB6AC))
    "technology" -> CategoryTheme("💻", Color(0xFFC7E2FB), Color(0xFF64B5F6))
    "space" -> CategoryTheme("🚀", Color(0xFFCBD0EC), Color(0xFF7986CB))
    "music" -> CategoryTheme("🎵", Color(0xFFFBCBDD), Color(0xFFF06292))
    "geography" -> CategoryTheme("🌍", Color(0xFFBFE3DE), Color(0xFF26A69A))
    "history" -> CategoryTheme("🏛️", Color(0xFFE3D6CC), Color(0xFF8D6E63))
    // Neutral fallback (no books motif) — a soft star instead.
    else -> CategoryTheme("⭐", Color(0xFFD7CCEC), Color(0xFF9575CD))
}

/**
 * Full-bleed themed background drawn behind a category's game grid. Replaces the plain
 * black/surface background with a soft vertical gradient and a few oversized, faint emoji
 * motifs so it reads as kid-friendly themed art while keeping the grid legible.
 *
 * To swap in a real photo later: drop a drawable named `bg_<category>` (e.g. bg_animals)
 * into res/drawable and this will render it (cropped to fill the portrait window) with a
 * light scrim instead of the gradient — no other code changes needed.
 */
@Composable
fun CategoryBackground(name: String, modifier: Modifier = Modifier) {
    val theme = categoryTheme(name)
    val context = LocalContext.current
    val key = name.trim().lowercase().replace(Regex("[^a-z0-9]"), "")
    val drawableId = remember(key) {
        if (key.isEmpty()) 0
        else context.resources.getIdentifier("bg_$key", "drawable", context.packageName)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.top, theme.bottom)))
    ) {
        if (drawableId != 0) {
            // A real themed image was provided — crop-fill the portrait window.
            Image(
                painter = painterResource(drawableId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Light scrim so letters/cards stay readable over the photo.
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
        } else {
            // No image: scatter faint emoji motifs for a playful themed feel.
            val motif = theme.emoji
            Text(motif, fontSize = 170.sp, modifier = Modifier.align(Alignment.TopStart).offset(x = (-34).dp, y = 24.dp).alpha(0.10f))
            Text(motif, fontSize = 120.sp, modifier = Modifier.align(Alignment.TopEnd).offset(x = 26.dp, y = 150.dp).alpha(0.08f))
            Text(motif, fontSize = 190.sp, modifier = Modifier.align(Alignment.BottomEnd).offset(x = 34.dp, y = 16.dp).alpha(0.10f))
            Text(motif, fontSize = 110.sp, modifier = Modifier.align(Alignment.BottomStart).offset(x = (-12).dp, y = (-24).dp).alpha(0.08f))
        }
    }
}
