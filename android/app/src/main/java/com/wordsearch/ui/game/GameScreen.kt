package com.wordsearch.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordsearch.data.model.GameSession
import com.wordsearch.ui.theme.WordFound
import com.wordsearch.ui.theme.WordSelected
import com.wordsearch.ui.theme.SelectBlue
import com.wordsearch.ui.theme.FoundWordPalette
import androidx.compose.ui.draw.drawBehind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    categoryId: String,
    gameMode: String = "CLASSIC",
    onNavigateBack: () -> Unit,
    onGameComplete: () -> Unit,
    onBottomNav: (com.wordsearch.ui.common.BottomDest) -> Unit = {},
    viewModel: GameViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val hintMessage by viewModel.hintMessage.collectAsState()
    val isCasualMode = gameMode == "CASUAL"

    // Show hint messages via snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(hintMessage) {
        hintMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearHintMessage()
        }
    }

    // Auto-save on back button for casual mode
    BackHandler {
        if (isCasualMode && uiState is GameUiState.Playing) {
            viewModel.saveCasualProgress()
        } else {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.startGame(categoryId, gameMode)
    }

    // Themed background behind the whole game screen, keyed to the current category.
    val bgCategory = (uiState as? GameUiState.Playing)?.session?.category ?: ""
    Box(modifier = Modifier.fillMaxSize()) {
    com.wordsearch.ui.common.CategoryBackground(bgCategory)
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    val s = (uiState as? GameUiState.Playing)?.session
                    if (s != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TopStat("Score", s.currentScore.toString())
                            TopStat("Words", "${s.wordsFound}/${s.targetWordCount}")
                            if (!isCasualMode) TopStat("Combo", "${s.currentCombo}x")
                        }
                    } else {
                        Text(com.wordsearch.ui.common.AppBranding.NAME)
                    }
                },
                actions = {
                    // Hint button — visible while playing
                    if (uiState is GameUiState.Playing) {
                        val coinBalance by viewModel.coinBalance.collectAsState()
                        IconButton(onClick = { viewModel.useHint() }) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Use hint ($coinBalance ⊙)"
                            )
                        }
                    }
                    if (!isCasualMode) {
                        IconButton(onClick = { viewModel.endGame() }) {
                            Icon(Icons.Default.Close, contentDescription = "End game")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    // Translucent surface scrim so the scoreboard reads as a clear banner
                    // while the themed background still shows through softly.
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                )
            )
        },
        bottomBar = {
            com.wordsearch.ui.common.AppBottomBar(current = null, onSelect = onBottomNav)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is GameUiState.Loading -> {
                    com.wordsearch.ui.common.BrandedLoadingScreen()
                }

                is GameUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Back to Categories")
                        }
                    }
                }

                is GameUiState.Playing -> {
                    GamePlayingContent(
                        session = state.session,
                        message = state.message,
                        isSuccess = state.isSuccess,
                        isBonus = state.isBonus,
                        gameMode = gameMode,
                        viewModel = viewModel
                    )
                }

                is GameUiState.LevelUp -> {
                    LevelUpDialog(
                        oldLevel = state.oldLevel,
                        newLevel = state.newLevel,
                        onContinue = { viewModel.continueAfterLevelUp() }
                    )
                }

                is GameUiState.GameOver -> {
                    GameOverScreen(
                        finalScore = state.finalScore,
                        wordsFound = state.wordsFound,
                        sessionDuration = state.sessionDuration,
                        casualBest = state.casualBest,
                        casualGames = state.casualGames,
                        casualWeekly = state.casualWeekly,
                        onPlayAgain = { viewModel.startGame(categoryId, gameMode) },
                        onMainMenu = onGameComplete
                    )
                }

                is GameUiState.CasualSaved -> {
                    CasualSavedScreen(
                        finalScore = state.finalScore,
                        wordsFound = state.wordsFound,
                        sessionDuration = state.sessionDuration,
                        onPlayAgain = { viewModel.startGame(categoryId, gameMode) },
                        onMainMenu = onGameComplete
                    )
                }
            }
        }
    }
    }
}

// Amber/gold tone for bonus word feedback
private val BonusGold = Color(0xFFF2A03D)
private val BonusGoldContainer = Color(0xFFFBE7CC)

@Composable
fun GamePlayingContent(
    session: GameSession,
    message: String?,
    isSuccess: Boolean?,
    isBonus: Boolean = false,
    gameMode: String,
    viewModel: GameViewModel
) {
    val selectedCells by viewModel.selectedCells.collectAsState()
    val foundWords by viewModel.foundWords.collectAsState()
    val bonusWords by viewModel.bonusWords.collectAsState()
    val foundWordPaths by viewModel.foundWordPaths.collectAsState()
    val paintBursts by viewModel.paintBursts.collectAsState()
    val lastWordResult by viewModel.lastWordResult.collectAsState()
    val hintedCells by viewModel.hintedCells.collectAsState()
    val isCasualMode = gameMode == "CASUAL"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // (Score / words / combo live in the top bar now.)

        // Word list at the top (found words strike through in place, so it never resizes
        // and the grid below stays put).
        HorizontalWordsList(
            words = session.words.map { it.word },
            foundWords = foundWords,
            isCasualMode = isCasualMode
        )

        // Weighted spacers above and below centre the grid in the space under the word
        // list, so it sits in the middle of the screen instead of stranded at the top.
        Spacer(modifier = Modifier.weight(1f))

        // Grid area — its size is locked to the screen WIDTH (a perfect square), so it
        // NEVER resizes or shifts when other UI (e.g. the bonus-word list) appears or the
        // header changes height. This is what keeps found tiles sitting exactly where they
        // were. The found-word popup overlays it without moving anything.
        val popup by viewModel.wordPopup.collectAsState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            WordGrid(
                grid = session.grid,
                selectedCells = selectedCells,
                foundWordPaths = foundWordPaths,
                hintedCells = hintedCells,
                paintBursts = paintBursts,
                onSelectionStart = { row, col -> viewModel.startSelection(row, col) },
                onSelectionUpdate = { row, col -> viewModel.updateSelection(row, col, session.gridSize) },
                onSelectionComplete = { viewModel.submitWord() }
            )
            WordFoundPopup(popup = popup, onDone = { viewModel.clearWordPopup() })
        }

        // Bonus (off-list) words appear BELOW the grid, so discovering one never moves the
        // grid above it.
        if (bonusWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            BonusWordsList(bonusWords = bonusWords)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun BoxScope.WordFoundPopup(popup: com.wordsearch.ui.game.WordPopup?, onDone: () -> Unit) {
    if (popup == null) return
    val scale = remember(popup.id) { Animatable(0.3f) }
    val alpha = remember(popup.id) { Animatable(0f) }
    LaunchedEffect(popup.id) {
        launch { alpha.animateTo(1f, tween(160)) }
        scale.animateTo(1.18f, tween(280, easing = FastOutSlowInEasing))
        scale.animateTo(1.0f, tween(160))
        delay(1300)                       // hold (~2s total)
        launch { scale.animateTo(0.9f, tween(280)) }
        alpha.animateTo(0f, tween(280))
        onDone()
    }
    Card(
        modifier = Modifier
            .align(Alignment.Center)
            .graphicsLayer {
                scaleX = scale.value; scaleY = scale.value; this.alpha = alpha.value
            },
        colors = CardDefaults.cardColors(
            containerColor = when {
                popup.isError -> MaterialTheme.colorScheme.errorContainer
                popup.isBonus -> BonusGoldContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val accent = when {
                popup.isError -> MaterialTheme.colorScheme.error
                popup.isBonus -> BonusGold
                else -> MaterialTheme.colorScheme.primary
            }
            if (popup.isBonus) {
                Text("BONUS!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = accent)
            }
            Text(
                popup.word,
                style = if (popup.isError) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = accent
            )
            popup.score?.let { s ->
                Text(
                    "+$s" + (if (popup.coins > 0) "   +${popup.coins}⊙" else ""),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BonusWordFeedback(
    message: String,
    lastWordResult: LastWordResult?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BonusGoldContainer),
        border = BorderStroke(1.5.dp, BonusGold)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6D4C00), // dark amber text
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (lastWordResult != null) {
                ScoreBreakdownRow(lastWordResult)
            }
        }
    }
}

@Composable
private fun ScoreBreakdownRow(result: LastWordResult) {
    val breakdown = result.scoreBreakdown
    if (breakdown.isEmpty()) return

    val base = breakdown["base"] ?: 0
    val lengthBonus = breakdown["lengthBonus"] ?: 0
    val comboMultiplier = breakdown["comboMultiplier"] ?: 1

    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$base  +$lengthBonus  ×$comboMultiplier = ${result.score}",
            style = MaterialTheme.typography.labelSmall,
            color = if (result.isBonus) Color(0xFF8D6000) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (result.coinsEarned > 0) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "+${result.coinsEarned}⊙",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8860B)
            )
        }
    }
}

@Composable
private fun BonusWordsList(bonusWords: Set<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BonusGoldContainer),
        border = BorderStroke(1.dp, BonusGold)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = "Bonus Words (${bonusWords.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6D4C00)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                bonusWords.forEach { word ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BonusGold.copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, BonusGold)
                    ) {
                        Text(
                            text = word.uppercase(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF6D4C00)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TopStat(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameStatsRow(session: GameSession, isCasualMode: Boolean, bonusWordCount: Int = 0) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCasualMode) Arrangement.SpaceEvenly else Arrangement.SpaceAround
    ) {
        StatChip(
            icon = Icons.Default.Star,
            label = "Score",
            value = session.currentScore.toString()
        )
        StatChip(
            icon = Icons.Default.Check,
            label = "Words",
            value = "${session.wordsFound}/${session.targetWordCount}"
        )
        // Only show combo in classic mode
        if (!isCasualMode) {
            StatChip(
                icon = Icons.Default.Favorite,
                label = "Combo",
                value = "${session.currentCombo}x"
            )
        }
        // Show bonus word count whenever any bonus words have been found
        if (bonusWordCount > 0) {
            BonusStatChip(count = bonusWordCount)
        }
    }
}

@Composable
fun BonusStatChip(count: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BonusGoldContainer,
        border = BorderStroke(1.dp, BonusGold)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = BonusGold
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6D4C00)
            )
            Text(
                text = " Bonus",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6D4C00)
            )
        }
    }
}

@Composable
fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = " $label",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// Distinct colour for hinted-word highlight (purple-ish)
private val HintHighlight = Color(0xFF8B7FD6)

@Composable
fun WordGrid(
    grid: List<List<Char>>,
    selectedCells: List<Pair<Int, Int>>,
    foundWordPaths: List<Pair<String, List<Pair<Int, Int>>>>,
    hintedCells: List<Pair<Int, Int>> = emptyList(),
    paintBursts: List<PaintBurst> = emptyList(),
    onSelectionStart: (Int, Int) -> Unit,
    onSelectionUpdate: (Int, Int) -> Unit,
    onSelectionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cols = grid[0].size
    val rows = grid.size
    val density = LocalDensity.current

    // ONE measured square is the single source of truth for cell geometry. The parent
    // already constrains us to a square, so we just take the largest square that fits and
    // derive cellPx (touch + line) and cellDp (tiles) from it. No second aspectRatio, no
    // onGloballyPositioned, no weight-based layout — so touch, the connecting line, and the
    // tiles share one identical origin and cell size and can never drift apart.
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val sideDp = minOf(maxWidth, maxHeight)
        val cellPx = with(density) { sideDp.toPx() } / cols
        val cellDp = sideDp / cols

        Box(modifier = Modifier
            .size(sideDp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(cols, rows, cellPx) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val row = (offset.y / cellPx).toInt().coerceIn(0, rows - 1)
                        val col = (offset.x / cellPx).toInt().coerceIn(0, cols - 1)
                        onSelectionStart(row, col)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val row = (change.position.y / cellPx).toInt().coerceIn(0, rows - 1)
                        val col = (change.position.x / cellPx).toInt().coerceIn(0, cols - 1)
                        onSelectionUpdate(row, col)
                    },
                    onDragEnd = { onSelectionComplete() },
                    onDragCancel = { }
                )
            }
        ) {
            // Per-cell highlight state
            val selectedSet = selectedCells.toHashSet()
            val hintedSet = hintedCells.toHashSet()
            val foundColorByCell = HashMap<Pair<Int, Int>, Color>()
            val foundOrderByCell = HashMap<Pair<Int, Int>, Int>()
            foundWordPaths.forEachIndexed { index, (_, path) ->
                val c = getWordColor(index)
                path.forEachIndexed { i, cell ->
                    foundColorByCell[cell] = c
                    foundOrderByCell[cell] = i
                }
            }

            // 1) Connecting line BEHIND the tiles — uses the SAME cellPx/origin as touch.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = cellPx * 0.42f
                fun center(cell: Pair<Int, Int>) =
                    Offset((cell.second + 0.5f) * cellPx, (cell.first + 0.5f) * cellPx)
                foundWordPaths.forEachIndexed { index, (_, path) ->
                    if (path.size >= 2) {
                        drawLine(
                            getWordColor(index), center(path.first()), center(path.last()),
                            strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }
                if (selectedCells.size >= 2) {
                    drawLine(
                        SelectBlue, center(selectedCells.first()), center(selectedCells.last()),
                        strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // 2) Tile BACKGROUNDS — explicit cellDp sizing (not weight) so every tile lines
            //    up 1:1 with the touch cell and the line. Letters are drawn later, ON TOP of
            //    the paint, so paint never hides them.
            Column(modifier = Modifier.fillMaxSize()) {
                grid.forEachIndexed { r, row ->
                    Row(modifier = Modifier.fillMaxWidth().height(cellDp)) {
                        row.forEachIndexed { c, _ ->
                            val cell = r to c
                            val isSelected = selectedSet.contains(cell)
                            val isHinted = hintedSet.contains(cell)
                            val isFound = !isSelected && !isHinted && foundColorByCell[cell] != null
                            val tileColor = when {
                                isSelected -> SelectBlue
                                isHinted -> HintHighlight
                                isFound -> foundColorByCell[cell]!!
                                else -> MaterialTheme.colorScheme.surface
                            }
                            GridCell(Modifier.width(cellDp).fillMaxHeight(), tileColor, isFound)
                        }
                    }
                }
            }

            // 3) Liquid paint — each found word launches droplets that shoot UP from its
            //    cells, arc over (gravity), and SPLAT onto other tiles where they squash,
            //    throw satellites and settle with depth + a wet sheen. Droplet count scales
            //    with THIS word's score. Splats persist for the rest of the board.
            val scope = rememberCoroutineScope()
            val drops = remember { mutableStateListOf<PaintDrop>() }
            var processed by remember { mutableStateOf(0) }
            LaunchedEffect(foundWordPaths.isEmpty()) {
                if (foundWordPaths.isEmpty()) { drops.clear(); processed = 0 }
            }
            LaunchedEffect(paintBursts.size) {
                while (processed < paintBursts.size) {
                    val burst = paintBursts[processed]; processed++
                    if (burst.cells.isEmpty()) continue
                    val color = getWordColor(burst.colorIndex)
                    val count = (burst.score / 130).coerceIn(3, 22) // proportional to word score
                    repeat(count) {
                        val (cr, cc) = burst.cells.random()
                        val ox = (cc + 0.5f) * cellPx
                        val oy = (cr + 0.5f) * cellPx
                        // Land a short hop away on neighbouring tiles (radiate outward).
                        val ang = kotlin.random.Random.nextFloat() * 6.2832f
                        val dist = cellPx * (0.8f + kotlin.random.Random.nextFloat() * 1.9f)
                        val drop = PaintDrop(
                            ox = ox, oy = oy,
                            dx = kotlin.math.cos(ang) * dist,
                            dy = kotlin.math.sin(ang) * dist,
                            arc = cellPx * (1.1f + kotlin.random.Random.nextFloat() * 2.0f), // shoot up
                            core = cellPx * (0.12f + kotlin.random.Random.nextFloat() * 0.12f),
                            color = color,
                            seed = kotlin.random.Random.nextFloat(),
                            life = Animatable(0f)
                        )
                        drops.add(drop)
                        scope.launch {
                            delay((kotlin.random.Random.nextFloat() * 120f).toLong())
                            drop.life.animateTo(1f, tween(820, easing = LinearEasing))
                        }
                    }
                    // Cap total persistent drops so very long games stay smooth.
                    while (drops.size > 420) drops.removeAt(0)
                }
            }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drops.forEach { d -> drawPaintDrop(d) }
            }

            // 4) Letters ON TOP of the paint, each with a crisp outline (the "bevel") so they
            //    stay readable over any paint colour. One outline (Stroke) pass + one fill
            //    pass per glyph, drawn via the text measurer. Layouts are cached per char.
            val measurer = rememberTextMeasurer()
            val onSurface = MaterialTheme.colorScheme.onSurface
            val fontSp = (cellDp.value * 0.5f).sp
            val baseStyle = remember(fontSp) {
                TextStyle(fontSize = fontSp, fontWeight = FontWeight.Black)
            }
            val strokeStyle = remember(fontSp, cellPx) {
                TextStyle(fontSize = fontSp, fontWeight = FontWeight.Black, drawStyle = Stroke(width = cellPx * 0.10f))
            }
            val fillCache = remember(fontSp) { HashMap<Char, TextLayoutResult>() }
            val strokeCache = remember(fontSp, cellPx) { HashMap<Char, TextLayoutResult>() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (r in grid.indices) {
                    for (c in grid[r].indices) {
                        val ch = grid[r][c]
                        val cell = r to c
                        val fill = when {
                            selectedSet.contains(cell) -> Color.White
                            hintedSet.contains(cell) -> Color.White
                            foundColorByCell[cell] != null -> Color.White
                            else -> onSurface
                        }
                        val outline = if (fill.luminance() > 0.5f) Color(0xFF1B1A2E) else Color.White
                        val fillLayout = fillCache.getOrPut(ch) { measurer.measure(ch.uppercase(), baseStyle) }
                        val strokeLayout = strokeCache.getOrPut(ch) { measurer.measure(ch.uppercase(), strokeStyle) }
                        val cx = (c + 0.5f) * cellPx
                        val cy = (r + 0.5f) * cellPx
                        drawText(
                            strokeLayout, color = outline,
                            topLeft = Offset(cx - strokeLayout.size.width / 2f, cy - strokeLayout.size.height / 2f)
                        )
                        drawText(
                            fillLayout, color = fill,
                            topLeft = Offset(cx - fillLayout.size.width / 2f, cy - fillLayout.size.height / 2f)
                        )
                    }
                }
            }
        }
    }
}

/** A single liquid paint droplet: ballistic flight from [ox],[oy] to a landing offset, then a persistent splat. */
private class PaintDrop(
    val ox: Float, val oy: Float,
    val dx: Float, val dy: Float,
    val arc: Float,
    val core: Float,
    val color: Color,
    val seed: Float,
    val life: Animatable<Float, AnimationVector1D>
)

/** Fraction of a droplet's life spent in flight; the rest is the impact splat + settle. */
private const val PAINT_FLIGHT = 0.55f

private fun DrawScope.drawPaintDrop(d: PaintDrop) {
    val t = d.life.value
    val light = lerp(d.color, Color.White, 0.38f)
    val dark = lerp(d.color, Color.Black, 0.18f)
    if (t < PAINT_FLIGHT) {
        // --- In flight: a stretched teardrop arcing up then down ---
        val f = t / PAINT_FLIGHT
        val x = d.ox + d.dx * f
        val y = d.oy + d.dy * f - d.arc * kotlin.math.sin(Math.PI.toFloat() * f)
        // velocity direction (for the tail)
        val vx = d.dx
        val vy = d.dy - d.arc * Math.PI.toFloat() * kotlin.math.cos(Math.PI.toFloat() * f)
        val vlen = kotlin.math.hypot(vx, vy).coerceAtLeast(0.0001f)
        val ux = vx / vlen; val uy = vy / vlen
        val r = d.core * 0.7f
        // tail
        drawLine(d.color.copy(alpha = 0.5f), Offset(x - ux * r * 2.2f, y - uy * r * 2.2f), Offset(x, y), strokeWidth = r * 1.1f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(d.color, radius = r, center = Offset(x, y))
        drawCircle(Color.White.copy(alpha = 0.45f), radius = r * 0.3f, center = Offset(x - r * 0.25f, y - r * 0.3f))
    } else {
        // --- Landed splat: squash overshoot then settle, with depth + sheen ---
        val s = (t - PAINT_FLIGHT) / (1f - PAINT_FLIGHT)   // 0..1
        val bloom = when {
            s < 0.30f -> (s / 0.30f) * 1.18f
            s < 0.60f -> 1.18f - 0.18f * ((s - 0.30f) / 0.30f)
            else -> 1f
        }
        val lx = d.ox + d.dx
        val ly = d.oy + d.dy
        val r = d.core * bloom
        // soft drop shadow (depth)
        drawCircle(Color.Black.copy(alpha = 0.12f), radius = r * 1.06f, center = Offset(lx + r * 0.12f, ly + r * 0.20f))
        // core with radial gradient: bright toward top-left light, darker at far rim
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(light, d.color, dark),
                center = Offset(lx - r * 0.3f, ly - r * 0.3f),
                radius = r * 1.5f
            ),
            radius = r,
            center = Offset(lx, ly)
        )
        // satellite droplets flung on impact
        for (k in 0 until 3) {
            val a = d.seed * 6.2832f + k * 2.094f
            val dd = r * (1.3f + 0.4f * k)
            drawCircle(dark.copy(alpha = 0.9f), radius = r * (0.26f - k * 0.05f), center = Offset(lx + kotlin.math.cos(a) * dd, ly + kotlin.math.sin(a) * dd))
        }
        // wet specular sheen (upper-left)
        drawOval(
            color = Color.White.copy(alpha = 0.5f),
            topLeft = Offset(lx - r * 0.5f, ly - r * 0.6f),
            size = Size(r * 0.6f, r * 0.38f)
        )
    }
}

// Generate different colors for each found word.
// Plain function (no composition) so it can be used inside DrawScope/drawBehind.
fun getWordColor(index: Int): Color {
    return FoundWordPalette[index % FoundWordPalette.size]
}

@Composable
fun GridCell(
    modifier: Modifier = Modifier,
    tileColor: Color = Color.White,
    isFound: Boolean = false
) {
    // Just the tile BACKGROUND — letters are drawn in a separate layer above the paint.
    // Highlight is a colour change plus a centred SCALE pop; the tile never translates or
    // rotates, so it can't leave its grid cell (that was the old misalignment bug).
    val animColor by animateColorAsState(tileColor, tween(220), label = "tileColor")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(isFound) {
        if (isFound) {
            pop.snapTo(0.62f)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        } else {
            pop.snapTo(1f)
        }
    }
    Box(
        modifier = modifier
            .padding(1.dp)
            .graphicsLayer { scaleX = pop.value; scaleY = pop.value } // centred → no drift
            .clip(RoundedCornerShape(10.dp))
            .background(animColor)
    )
}

@Composable
fun HorizontalWordsList(
    words: List<String>,
    foundWords: Set<String>,
    isCasualMode: Boolean
) {
    // Separate and sort words: unfound first, then found at the end
    val (unfoundWords, foundWordsList) = words.partition { !foundWords.contains(it.lowercase()) }
    val sortedWords = unfoundWords + foundWordsList

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCasualMode) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isCasualMode) "Find These Words" else "Words to Find",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isCasualMode) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Horizontal scrollable row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sortedWords.forEach { word ->
                    val isFound = foundWords.contains(word.lowercase())
                    WordChip(
                        word = word,
                        isFound = isFound,
                        isCasualMode = isCasualMode
                    )
                }
            }
        }
    }
}

@Composable
fun WordChip(
    word: String,
    isFound: Boolean,
    isCasualMode: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isFound) {
            Color(0xFF4CAF50).copy(alpha = 0.2f) // Green background for found words
        } else {
            if (isCasualMode) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isFound) {
                Color(0xFF4CAF50) // Green border
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isFound) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50), // Green
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = word.uppercase(),
                style = MaterialTheme.typography.bodySmall,
                textDecoration = if (isFound) TextDecoration.LineThrough else null,
                color = if (isFound) {
                    Color(0xFF4CAF50) // Green text
                } else {
                    if (isCasualMode) {
                        MaterialTheme.colorScheme.onTertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                },
                fontWeight = if (!isFound) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

@Composable
fun LevelUpDialog(
    oldLevel: Int,
    newLevel: Int,
    onContinue: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        icon = {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Level Up!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Congratulations! You've reached level $newLevel!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("Continue")
            }
        }
    )
}

@Composable
fun GameOverScreen(
    finalScore: Int,
    wordsFound: Int,
    sessionDuration: Int,
    casualBest: Long = 0,
    casualGames: Int = 0,
    casualWeekly: Long = 0,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
    val isCasual = casualGames > 0
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Game Complete!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GameOverStat("This Game", finalScore.toString())
                Spacer(modifier = Modifier.height(16.dp))
                GameOverStat("Words Found", wordsFound.toString())
                Spacer(modifier = Modifier.height(16.dp))
                GameOverStat("Time", "${sessionDuration}s")
            }
        }

        if (isCasual) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Casual Stats",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    GameOverStat("This Week", casualWeekly.toString())
                    Spacer(modifier = Modifier.height(16.dp))
                    GameOverStat("Best Run", casualBest.toString())
                    Spacer(modifier = Modifier.height(16.dp))
                    GameOverStat("Games Played", casualGames.toString())
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play Again")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onMainMenu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Main Menu")
        }
    }
}

@Composable
fun GameOverStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun CasualSavedScreen(
    finalScore: Int,
    wordsFound: Int,
    sessionDuration: Int,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Save,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Progress Saved!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Take a break and come back anytime to continue",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CasualSavedStat("Current Score", finalScore.toString())
                Spacer(modifier = Modifier.height(16.dp))
                CasualSavedStat("Words Found", wordsFound.toString())
                Spacer(modifier = Modifier.height(16.dp))
                CasualSavedStat("Time Played", "${sessionDuration}m")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onPlayAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Play Again")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onMainMenu,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Main Menu")
        }
    }
}

@Composable
fun CasualSavedStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}
