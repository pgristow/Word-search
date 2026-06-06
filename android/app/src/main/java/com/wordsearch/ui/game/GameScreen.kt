package com.wordsearch.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
private val BonusGold = Color(0xFFFFC107)
private val BonusGoldContainer = Color(0xFFFFF8E1)

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
                            if (isSuccess == true && lastWordResult != null) {
                                ScoreBreakdownRow(lastWordResult)
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
private val HintHighlight = Color(0xFF9C27B0)

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
        val cellSize = with(density) { (maxWidth / grid[0].size.toFloat()).toPx() }

        // Draw the grid with lines behind letters
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E1)) // Cream background
            .drawBehind {
                // Draw found word paths
                foundWordPaths.forEachIndexed { index, (word, path) ->
                    if (path.size >= 1) {
                        val color = getWordColor(index).copy(alpha = 0.5f)

                        if (path.size == 1) {
                            // Single letter word - draw a circle
                            val centerX = (path[0].second + 0.5f) * cellSize
                            val centerY = (path[0].first + 0.5f) * cellSize
                            drawCircle(
                                color = color,
                                radius = cellSize * 0.4f,
                                center = Offset(centerX, centerY)
                            )
                        } else {
                            // Multi-letter word - draw line from first to last with extension
                            val firstCell = path.first()
                            val lastCell = path.last()

                            val firstX = (firstCell.second + 0.5f) * cellSize
                            val firstY = (firstCell.first + 0.5f) * cellSize
                            val lastX = (lastCell.second + 0.5f) * cellSize
                            val lastY = (lastCell.first + 0.5f) * cellSize

                            // Calculate direction vector
                            val dx = lastX - firstX
                            val dy = lastY - firstY
                            val length = kotlin.math.sqrt(dx * dx + dy * dy)

                            // Normalize and extend by 0.5 cells in each direction
                            val extension = cellSize * 0.5f
                            val ndx = (dx / length) * extension
                            val ndy = (dy / length) * extension

                            val startX = firstX - ndx
                            val startY = firstY - ndy
                            val endX = lastX + ndx
                            val endY = lastY + ndy

                            drawLine(
                                color = color,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = cellSize * 0.8f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }

                // Draw current selection
                if (selectedCells.size >= 1) {
                    val selectionColor = Color(0xFF2196F3).copy(alpha = 0.5f) // Blue

                    if (selectedCells.size == 1) {
                        // Single letter - draw a circle
                        val centerX = (selectedCells[0].second + 0.5f) * cellSize
                        val centerY = (selectedCells[0].first + 0.5f) * cellSize
                        drawCircle(
                            color = selectionColor,
                            radius = cellSize * 0.4f,
                            center = Offset(centerX, centerY)
                        )
                    } else {
                        // Multi-letter - draw line from first to last with extension
                        val firstCell = selectedCells.first()
                        val lastCell = selectedCells.last()

                        val firstX = (firstCell.second + 0.5f) * cellSize
                        val firstY = (firstCell.first + 0.5f) * cellSize
                        val lastX = (lastCell.second + 0.5f) * cellSize
                        val lastY = (lastCell.first + 0.5f) * cellSize

                        // Calculate direction vector
                        val dx = lastX - firstX
                        val dy = lastY - firstY
                        val length = kotlin.math.sqrt(dx * dx + dy * dy)

                        // Normalize and extend by 0.5 cells in each direction
                        val extension = cellSize * 0.5f
                        val ndx = (dx / length) * extension
                        val ndy = (dy / length) * extension

                        val startX = firstX - ndx
                        val startY = firstY - ndy
                        val endX = lastX + ndx
                        val endY = lastY + ndy

                        drawLine(
                            color = selectionColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = cellSize * 0.8f,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                }

                // Draw hinted cells (pulsing purple highlight over each cell)
                if (hintedCells.isNotEmpty()) {
                    val hintColor = HintHighlight.copy(alpha = 0.45f)
                    hintedCells.forEach { (row, col) ->
                        val centerX = (col + 0.5f) * cellSize
                        val centerY = (row + 0.5f) * cellSize
                        drawCircle(
                            color = hintColor,
                            radius = cellSize * 0.42f,
                            center = Offset(centerX, centerY)
                        )
                    }
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                grid.forEachIndexed { rowIndex, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        row.forEachIndexed { colIndex, char ->
                            GridCell(char)
                        }
                    }
                }
            }
        }
    }
}

// Generate different colors for each found word
@Composable
fun getWordColor(index: Int): Color {
    val colors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF00BCD4), // Cyan
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF3F51B5), // Indigo
        Color(0xFFE91E63), // Pink
    )
    return colors[index % colors.size]
}

@Composable
fun GridCell(char: Char) {
    Box(
        modifier = Modifier
            .size(56.dp) // Even larger cells for better visibility
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp // Explicit large font size
            ),
            fontWeight = FontWeight.Black, // Thickest font weight
            color = MaterialTheme.colorScheme.onSurface
        )
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
