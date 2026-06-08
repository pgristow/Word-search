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
import androidx.compose.runtime.remember
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
 * Animated paint drips running down from the top edge — they extend on entry and a bulb
 * of paint gathers at the bottom of each, like wet paint dripping into the screen.
 */
@Composable
private fun PaintDripsOverlay(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
    }
    Canvas(modifier = modifier) {
        TopDrips.forEach { d ->
            val x = d.xFrac * size.width
            val w = d.widthDp.dp.toPx()
            val len = d.lenDp.dp.toPx() * progress.value
            // running paint
            drawLine(d.color, Offset(x, -8f), Offset(x, len), strokeWidth = w, cap = StrokeCap.Round)
            // bulb gathering at the tip
            val bulb = w * 0.75f + 4f
            drawCircle(d.color, radius = bulb, center = Offset(x, len))
            // wet sheen on the bulb
            drawCircle(Color.White.copy(alpha = 0.35f), radius = bulb * 0.32f, center = Offset(x - bulb * 0.3f, len - bulb * 0.35f))
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
