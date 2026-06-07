package com.wordsearch.ui.leaderboard

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordsearch.data.model.LeaderboardRowDto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onBottomNav: (com.wordsearch.ui.common.BottomDest) -> Unit = {},
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            com.wordsearch.ui.common.AppBottomBar(current = com.wordsearch.ui.common.BottomDest.LEADERBOARD, onSelect = onBottomNav)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            val tabs = LeaderboardTab.values()
            TabRow(
                selectedTabIndex = tabs.indexOf(selectedTab)
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.label) }
                    )
                }
            }

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is LeaderboardUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    is LeaderboardUiState.Error -> {
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
                            Button(onClick = { viewModel.loadLeaderboard() }) {
                                Text("Retry")
                            }
                        }
                    }

                    is LeaderboardUiState.Success -> {
                        if (state.rows.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No leaderboard entries yet.\nBe the first to play!",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LeaderboardContent(
                                rows = state.rows,
                                currentUserId = state.currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardContent(
    rows: List<LeaderboardRowDto>,
    currentUserId: String?
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top 3 podium
        if (rows.size >= 3) {
            PodiumSection(
                first = rows[0],
                second = rows[1],
                third = rows[2]
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Rest of the leaderboard
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val startIndex = if (rows.size >= 3) 3 else 0
            items(rows.drop(startIndex)) { row ->
                LeaderboardRowCard(
                    row = row,
                    isCurrentUser = row.userId == currentUserId
                )
            }
        }
    }
}

@Composable
fun PodiumSection(
    first: LeaderboardRowDto,
    second: LeaderboardRowDto,
    third: LeaderboardRowDto
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Top 3 Players",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Second place
                PodiumPosition(
                    row = second,
                    rank = 2,
                    color = Color(0xFFC0C0C0),
                    height = 80.dp
                )

                // First place
                PodiumPosition(
                    row = first,
                    rank = 1,
                    color = Color(0xFFFFD700),
                    height = 120.dp
                )

                // Third place
                PodiumPosition(
                    row = third,
                    rank = 3,
                    color = Color(0xFFCD7F32),
                    height = 60.dp
                )
            }
        }
    }
}

@Composable
fun PodiumPosition(
    row: LeaderboardRowDto,
    rank: Int,
    color: Color,
    height: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Avatar/Icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (rank) {
                    1 -> Icons.Default.Star
                    2 -> Icons.Default.Favorite
                    else -> Icons.Default.Info
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = row.username,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Podium block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(color.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = row.score.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LeaderboardRowCard(
    row: LeaderboardRowDto,
    isCurrentUser: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrentUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${row.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Username
            Text(
                text = row.username,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                color = if (isCurrentUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            // Score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = row.score.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrentUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    text = "points",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
