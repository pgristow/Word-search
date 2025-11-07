package com.wordsearch.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordsearch.data.model.GameSession
import com.wordsearch.ui.theme.WordFound
import com.wordsearch.ui.theme.WordSelected

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

    LaunchedEffect(Unit) {
        viewModel.startGame(categoryId, gameMode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Word Search") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.endGame() }) {
                        Icon(Icons.Default.Close, contentDescription = "End game")
                    }
                }
            )
        }
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

@Composable
fun GamePlayingContent(
    session: GameSession,
    message: String?,
    isSuccess: Boolean?,
    gameMode: String,
    viewModel: GameViewModel
) {
    val selectedCells by viewModel.selectedCells.collectAsState()
    val foundWords by viewModel.foundWords.collectAsState()
    val isCasualMode = gameMode == "CASUAL"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Score and combo
        GameStatsRow(session, isCasualMode)

        Spacer(modifier = Modifier.height(16.dp))

        // Feedback message
        AnimatedVisibility(
            visible = message != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            message?.let {
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
                    Text(
                        text = it,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Word grid
        WordGrid(
            grid = session.grid,
            selectedCells = selectedCells,
            onCellSelected = { row, col -> viewModel.onCellSelected(row, col) },
            onSelectionComplete = { viewModel.submitWord() },
            modifier = Modifier.weight(1f, fill = false)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.clearSelection() },
                modifier = Modifier.weight(1f),
                enabled = selectedCells.isNotEmpty()
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear")
            }
            Button(
                onClick = { viewModel.submitWord() },
                modifier = Modifier.weight(1f),
                enabled = selectedCells.size >= 3
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Submit")
            }
        }

        // Save button for casual mode
        if (isCasualMode) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.saveCasualProgress() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save & Exit")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Words list
        WordsList(
            words = session.words.map { it.word },
            foundWords = foundWords,
            isCasualMode = isCasualMode,
            modifier = Modifier.weight(0.5f)
        )
    }
}

@Composable
fun GameStatsRow(session: GameSession, isCasualMode: Boolean) {
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

@Composable
fun WordGrid(
    grid: List<List<Char>>,
    selectedCells: List<Pair<Int, Int>>,
    onCellSelected: (Int, Int) -> Unit,
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
                            onCellSelected(row, col)
                        }
                    },
                    onDrag = { change, _ ->
                        if (gridSize != Offset.Zero) {
                            val cellSize = gridSize.x / grid[0].size
                            val row = (change.position.y / cellSize).toInt().coerceIn(0, grid.size - 1)
                            val col = (change.position.x / cellSize).toInt().coerceIn(0, grid[0].size - 1)
                            onCellSelected(row, col)
                        }
                    },
                    onDragEnd = {
                        onSelectionComplete()
                    }
                )
            }
    ) {
        val cellSize = with(density) { (maxWidth / grid[0].size.toFloat()).toPx() }

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
                        val isSelected = selectedCells.contains(rowIndex to colIndex)
                        GridCell(char, isSelected)
                    }
                }
            }
        }
    }
}

@Composable
fun GridCell(char: Char, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) WordSelected else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char.uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun WordsList(
    words: List<String>,
    foundWords: Set<String>,
    isCasualMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCasualMode) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (isCasualMode) "Find These Words" else "Words to Find",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCasualMode) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(words) { word ->
                    val isFound = foundWords.contains(word.lowercase())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCasualMode) {
                            // Checkbox for casual mode
                            Checkbox(
                                checked = isFound,
                                onCheckedChange = null,
                                enabled = false,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            // Icons for classic mode
                            if (isFound) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = WordFound,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = word.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            textDecoration = if (isFound) TextDecoration.LineThrough else null,
                            color = if (isFound) {
                                if (isCasualMode) {
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                                } else {
                                    WordFound
                                }
                            } else {
                                if (isCasualMode) {
                                    MaterialTheme.colorScheme.onTertiaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            }
                        )
                    }
                }
            }
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
