package com.wordsearch.ui.landing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordsearch.ui.common.AppLogo

private val BgTop = Color(0xFF6A5AE0)
private val BgBottom = Color(0xFF8E7BEF)

// Playful drip colours (the logo palette).
private val DripYellow = Color(0xFFFFC83D)
private val DripPink = Color(0xFFFF5E8A)
private val DripWhite = Color(0xFFFFFFFF)
private val DripLilac = Color(0xFFB6A9FF)

/** One paint drip: horizontal position (fraction of width), max length, thickness, colour. */
private data class Drip(val xFrac: Float, val lenDp: Float, val widthDp: Float, val color: Color)

private val TopDrips = listOf(
    Drip(0.06f, 150f, 18f, DripYellow),
    Drip(0.17f, 90f, 14f, DripPink),
    Drip(0.30f, 200f, 16f, DripWhite),
    Drip(0.43f, 70f, 15f, DripLilac),
    Drip(0.57f, 130f, 17f, DripYellow),
    Drip(0.70f, 95f, 14f, DripPink),
    Drip(0.83f, 175f, 16f, DripLilac),
    Drip(0.94f, 80f, 18f, DripWhite),
)

/**
 * Live paint drips along the top edge: each ooze extends in, its bulb gently swells, and on
 * a loop a droplet pinches off and falls down the screen (with gravity) before a new one
 * gathers — so the paint keeps dripping. Each drip runs on its own phase so it feels organic.
 */
@Composable
private fun PaintDripsOverlay(modifier: Modifier = Modifier) {
    // A continuously increasing clock (seconds) drives all the motion.
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now -> t = (now - start) / 1_000_000_000f }
        }
    }
    Canvas(modifier = modifier) {
        TopDrips.forEachIndexed { i, d ->
            val x = d.xFrac * size.width
            val w = d.widthDp.dp.toPx()
            val phase = i * 0.9f
            // Extend in over the first ~1.1s, then a small continuous ooze.
            val grow = (t / 1.1f).coerceIn(0f, 1f)
            val ooze = (kotlin.math.sin(t * 0.9f + phase) * 0.5f + 0.5f) * w * 0.9f
            val len = d.lenDp.dp.toPx() * grow + ooze
            // The running paint column.
            drawLine(d.color, Offset(x, -8f), Offset(x, len), strokeWidth = w, cap = StrokeCap.Round)
            // The bulb at the tip swells and shrinks (gathering paint).
            val swell = kotlin.math.sin(t * 1.6f + phase) * w * 0.18f
            val bulb = w * 0.78f + 4f + swell
            drawCircle(d.color, radius = bulb, center = Offset(x, len))
            drawCircle(Color.White.copy(alpha = 0.35f), radius = bulb * 0.30f, center = Offset(x - bulb * 0.3f, len - bulb * 0.32f))

            // A droplet pinches off and falls on a loop (faster/accelerating as it drops).
            val period = 3.2f + (i % 3) * 0.6f
            val k = ((t + phase) % period) / period   // 0..1 within the fall cycle
            if (grow >= 1f) {
                val fall = k * k                       // gravity ease-in
                val dy = len + fall * (size.height - len + 40f)
                val alpha = (1f - k).coerceIn(0f, 1f)
                val dropR = w * 0.55f * (1f - k * 0.35f)
                // teardrop: a short tail above the falling head
                drawLine(d.color.copy(alpha = alpha * 0.7f), Offset(x, dy - dropR * 2.2f), Offset(x, dy), strokeWidth = dropR * 1.1f, cap = StrokeCap.Round)
                drawCircle(d.color.copy(alpha = alpha), radius = dropR, center = Offset(x, dy))
                drawCircle(Color.White.copy(alpha = alpha * 0.4f), radius = dropR * 0.3f, center = Offset(x - dropR * 0.25f, dy - dropR * 0.3f))
            }
        }
    }
}

/**
 * Home/landing page: the WordPop logo up top and the primary entry points —
 * Competitive, Casual, Profile, Settings, Quit.
 */
@Composable
fun LandingScreen(
    onCasual: () -> Unit,
    onCompetitive: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onQuit: () -> Unit,
    viewModel: LandingViewModel = hiltViewModel()
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        // Decorative paint drips hanging from the top edge.
        PaintDripsOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1.2f))
            AppLogo(fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Hello, ${viewModel.username}",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.75f)
            )
            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.chooseCompetitive(); onCompetitive() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = BgTop)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Competitive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(
                onClick = { viewModel.chooseCasual(); onCasual() },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Casual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onProfile,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Profile", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.weight(1f).height(48.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Settings", style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onQuit,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
            ) {
                Text("Quit", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.weight(0.6f))
        }
    }
}
