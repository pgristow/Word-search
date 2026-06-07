package com.wordsearch.ui.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordsearch.data.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateToGame: (String, String) -> Unit,
    onNavigateToAchievements: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToDailyChallenge: () -> Unit,
    onNavigateToStore: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userProgress by viewModel.userProgress.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Word Search",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        userProgress?.let { progress ->
                            Text(
                                text = "Level ${progress.currentLevel} • ${progress.totalScore} pts",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Store") },
                            onClick = {
                                showMenu = false
                                onNavigateToStore()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ShoppingCart, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Logout") },
                            onClick = {
                                showMenu = false
                                viewModel.logout(onLogout)
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Shared bar so Categories matches every other page (same icons + styling).
            com.wordsearch.ui.common.AppBottomBar(
                current = com.wordsearch.ui.common.BottomDest.CATEGORIES,
                onSelect = { dest ->
                    when (dest) {
                        com.wordsearch.ui.common.BottomDest.DAILY -> onNavigateToDailyChallenge()
                        com.wordsearch.ui.common.BottomDest.CATEGORIES -> { /* already here */ }
                        com.wordsearch.ui.common.BottomDest.ACHIEVEMENTS -> onNavigateToAchievements()
                        com.wordsearch.ui.common.BottomDest.LEADERBOARD -> onNavigateToLeaderboard()
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
                is CategoriesUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is CategoriesUiState.Error -> {
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
                        Button(onClick = { viewModel.loadData() }) {
                            Text("Retry")
                        }
                    }
                }

                is CategoriesUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // User stats card
                        userProgress?.let { progress ->
                            UserStatsCard(progress)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Categories header
                        Text(
                            text = "Choose a Category",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Categories grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.categories) { category ->
                                CategoryCard(
                                    category = category,
                                    userLevel = userProgress?.currentLevel ?: 1,
                                    onClick = { onNavigateToGame(category.id, category.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserStatsCard(progress: com.wordsearch.data.model.UserProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatItem(
                icon = Icons.Default.Star,
                label = "Score",
                value = progress.totalScore.toString()
            )
            StatItem(
                icon = Icons.Default.Check,
                label = "Words",
                value = progress.totalWordsFound.toString()
            )
            StatItem(
                icon = Icons.Default.Favorite,
                label = "Streak",
                value = progress.currentStreak.toString()
            )
            StatItem(
                icon = Icons.Default.Person,
                label = "Level",
                value = progress.currentLevel.toString()
            )
        }
    }
}

@Composable
fun StatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCard(
    category: Category,
    userLevel: Int,
    onClick: () -> Unit
) {
    val isLocked = userLevel < category.unlockLevel

    Card(
        onClick = { if (!isLocked) onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        border = if (!isLocked) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null,
        enabled = !isLocked
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Cartoon-style category badge: a big emoji on a soft tinted circle.
                val art = categoryArt(category.name)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(art.tint.copy(alpha = if (isLocked) 0.12f else 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLocked) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(text = art.emoji, fontSize = 40.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isLocked) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isLocked) {
                    Text(
                        text = "Level ${category.unlockLevel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "${category.wordCount} words",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/** Cartoon-style art (emoji + accent colour) for a category, matched by name. */
private data class CategoryArt(val emoji: String, val tint: Color)

private fun categoryArt(name: String): CategoryArt = when (name.trim().lowercase()) {
    "animals" -> CategoryArt("🦁", Color(0xFFFF8A3D))
    "food" -> CategoryArt("🍔", Color(0xFFE3563B))
    "sports" -> CategoryArt("⚽", Color(0xFF2E7D32))
    "science" -> CategoryArt("🔬", Color(0xFF6A1B9A))
    "nature" -> CategoryArt("🌿", Color(0xFF2E9E6B))
    "technology" -> CategoryArt("💻", Color(0xFF1565C0))
    "space" -> CategoryArt("🚀", Color(0xFF3949AB))
    "music" -> CategoryArt("🎵", Color(0xFFC2185B))
    "geography" -> CategoryArt("🌍", Color(0xFF00897B))
    "history" -> CategoryArt("🏛️", Color(0xFF8D6E63))
    else -> CategoryArt("📚", Color(0xFF5C6BC0))
}
