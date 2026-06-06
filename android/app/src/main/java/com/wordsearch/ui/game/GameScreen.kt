package com.wordsearch.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.graphics.drawscope.Stroke
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Search") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isCasualMode && uiState is GameUiState.Playing) {
                            viewModel.saveCasualProgress()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                }
            )
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
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
                        onNavigateBack = onGameComplete
                    )
                }

                is GameUiState.CasualSaved -> {
                    CasualSavedScreen(
                        finalScore = state.finalScore,
                        wordsFound = state.wordsFound,
                        sessionDuration = state.sessionDuration,
                        onNavigateBack = onGameComplete
                    )
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
    val lastWordResult by viewModel.lastWordResult.collectAsState()
    val hintedCells by viewModel.hintedCells.collectAsState()
    val isCasualMode = gameMode == "CASUAL"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Score and combo
        GameStatsRow(session, isCasualMode, bonusWords.size)

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback message
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            message?.let {
                if (isBonus) {
                    // Gold/amber bonus word card
                    BonusWordFeedback(
                        message = it,
                        lastWordResult = lastWordResult
                    )
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSuccess == true) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Score breakdown for regular correct words
                            val result = lastWordResult
                            if (isSuccess == true && result != null) {
                                ScoreBreakdownRow(result)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Show selected word
        if (selectedCells.isNotEmpty()) {
            val selectedWord = buildString {
                selectedCells.forEach { (row, col) ->
                    if (row in session.grid.indices && col in session.grid[row].indices) {
                        append(session.grid[row][col])
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = "Selected: $selectedWord (${selectedCells.size} letters)",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Word grid
        WordGrid(
            grid = session.grid,
            selectedCells = selectedCells,
            foundWordPaths = foundWordPaths,
            hintedCells = hintedCells,
            onSelectionStart = { row, col -> viewModel.startSelection(row, col) },
            onSelectionUpdate = { row, col -> viewModel.updateSelection(row, col, session.gridSize) },
            onSelectionComplete = { viewModel.submitWord() },
            modifier = Modifier.weight(1f, fill = false)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal scrollable target word list
        HorizontalWordsList(
            words = session.words.map { it.word },
            foundWords = foundWords,
            isCasualMode = isCasualMode
        )

        // Bonus words section — only visible when at least one bonus word has been found
        if (bonusWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            BonusWordsList(bonusWords = bonusWords)
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
    onSelectionStart: (Int, Int) -> Unit,
    onSelectionUpdate: (Int, Int) -> Unit,
    onSelectionComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var gridSize by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onGloballyPositioned { coordinates ->
                gridSize = Offset(
                    coordinates.size.width.toFloat(),
                    coordinates.size.height.toFloat()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (gridSize != Offset.Zero) {
                            val cellSize = gridSize.x / grid[0].size
                            val row = (offset.y / cellSize).toInt().coerceIn(0, grid.size - 1)
                            val col = (offset.x / cellSize).toInt().coerceIn(0, grid[0].size - 1)
                            onSelectionStart(row, col)
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (gridSize != Offset.Zero) {
                            val cellSize = gridSize.x / grid[0].size
                            val row = (change.position.y / cellSize).toInt().coerceIn(0, grid.size - 1)
                            val col = (change.position.x / cellSize).toInt().coerceIn(0, grid[0].size - 1)
                            onSelectionUpdate(row, col)
                        }
                    },
                    onDragEnd = {
                        onSelectionComplete()
                    },
                    onDragCancel = {
                        // Clear selection on cancel
                    }
                )
            }
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            // Per-cell highlight state
            val selectedSet = selectedCells.toHashSet()
            val hintedSet = hintedCells.toHashSet()
            val foundColorByCell = HashMap<Pair<Int, Int>, Color>().apply {
                foundWordPaths.forEachIndexed { index, (_, path) ->
                    val c = getWordColor(index)
                    path.forEach { cell -> this[cell] = c }
                }
            }

            // 1) Connecting line BEHIND the tiles — shows through the gaps so you can
            //    see what's linked. Capped at the first/last tile centres (no bleed).
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = size.width / grid[0].size
                val stroke = cellSize * 0.60f
                fun center(cell: Pair<Int, Int>) =
                    Offset((cell.second + 0.5f) * cellSize, (cell.first + 0.5f) * cellSize)
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

            // 2) Letter tiles ON TOP — each tile is highlighted by its own state, so the
            //    actual selected tiles light up (not just a line).
            Column(modifier = Modifier.fillMaxSize()) {
                grid.forEachIndexed { r, row ->
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        row.forEachIndexed { c, char ->
                            val cell = r to c
                            val isSelected = selectedSet.contains(cell)
                            val isHinted = hintedSet.contains(cell)
                            val isFound = !isSelected && !isHinted && foundColorByCell[cell] != null
                            val tileColor: Color
                            val textColor: Color
                            when {
                                isSelected -> { tileColor = SelectBlue; textColor = Color.White }
                                isHinted -> { tileColor = HintHighlight; textColor = Color.White }
                                isFound -> { tileColor = foundColorByCell[cell]!!; textColor = Color.White }
                                else -> { tileColor = MaterialTheme.colorScheme.surface; textColor = MaterialTheme.colorScheme.onSurface }
                            }
                            GridCell(char, Modifier.weight(1f).fillMaxHeight(), tileColor, textColor, isFound)
                        }
                    }
                }
            }
        }
    }
}

// Generate different colors for each found word.
// Plain function (no composition) so it can be used inside DrawScope/drawBehind.
fun getWordColor(index: Int): Color {
    return FoundWordPalette[index % FoundWordPalette.size]
}

@Composable
fun GridCell(
    char: Char,
    modifier: Modifier = Modifier,
    tileColor: Color = Color.White,
    textColor: Color = Color.Black,
    isFound: Boolean = false
) {
    // Smoothly fade the tile colour; when a word is found the tile jumps up, flips, and
    // casts a shadow for a satisfying pop.
    val animColor by animateColorAsState(tileColor, tween(220), label = "tileColor")
    val flip = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    LaunchedEffect(isFound) {
        if (isFound) {
            flip.snapTo(0f); lift.snapTo(0f)
            launch {
                lift.animateTo(1f, keyframes {
                    durationMillis = 480
                    0f at 0
                    1f at 170
                    0f at 480
                })
            }
            flip.animateTo(360f, tween(480, easing = FastOutSlowInEasing))
        } else {
            flip.snapTo(0f); lift.snapTo(0f)
        }
    }
    // Fills the cell its parent allotted (via weight) so the visible tile lines up
    // exactly with the touch + highlight grid. Font scales with the tile so the bigger
    // grids stay legible.
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val fontSp = (maxWidth.value * 0.5f).sp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp)
                .graphicsLayer {
                    rotationY = flip.value
                    translationY = -lift.value * 14.dp.toPx()
                    shadowElevation = lift.value * 12.dp.toPx()
                    shape = RoundedCornerShape(10.dp)
                    clip = true
                }
                .background(animColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = char.uppercase(),
                fontSize = fontSp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1
            )
        }
    }
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
    onNavigateBack: () -> Unit
) {
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
                GameOverStat("Final Score", finalScore.toString())
                Spacer(modifier = Modifier.height(16.dp))
                GameOverStat("Words Found", wordsFound.toString())
                Spacer(modifier = Modifier.height(16.dp))
                GameOverStat("Time", "${sessionDuration}s")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Categories")
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
    onNavigateBack: () -> Unit
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
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            )
        ) {
            Text("Back to Categories")
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
