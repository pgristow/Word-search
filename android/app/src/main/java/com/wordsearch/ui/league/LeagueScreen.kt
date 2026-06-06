package com.wordsearch.ui.league

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wordsearch.data.model.LeagueMeResponse
import com.wordsearch.data.model.LeagueStandingDto

// Tier constants
private const val TIER_MASTER = 6
private const val TIER_BRONZE = 1
private const val PROMOTION_ZONE_SIZE = 7
private const val RELEGATION_ZONE_SIZE = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeagueScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeagueViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("League") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                is LeagueUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is LeagueUiState.Error -> {
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
                        Button(onClick = { viewModel.loadLeague() }) {
                            Text("Retry")
                        }
                    }
                }

                is LeagueUiState.Success -> {
                    LeagueContent(data = state.data)
                }
            }
        }
    }
}

@Composable
fun LeagueContent(data: LeagueMeResponse) {
    val standingCount = data.standings.size
    val hasPromotion = data.tierOrder < TIER_MASTER
    val hasRelegation = data.tierOrder > TIER_BRONZE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Tier badge header
        TierBadge(tierName = data.tierName)

        Spacer(modifier = Modifier.height(8.dp))

        // Week info
        Text(
            text = "Week: ${data.weekKey}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Zone legend
        ZoneLegend(hasPromotion = hasPromotion, hasRelegation = hasRelegation)

        Spacer(modifier = Modifier.height(12.dp))

        if (data.standings.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No standings available yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            // Standings list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(data.standings) { standing ->
                    val rowZone = resolveZone(
                        rank = standing.rank,
                        totalCount = standingCount,
                        hasPromotion = hasPromotion,
                        hasRelegation = hasRelegation
                    )
                    LeagueStandingRow(
                        standing = standing,
                        isCurrentUser = standing.userId == data.myUserId,
                        zone = rowZone
                    )
                }
            }
        }
    }
}

/**
 * Determines whether a row falls in the promotion zone, relegation zone, or neither.
 * Promotion: top [PROMOTION_ZONE_SIZE] ranks (only when tierOrder < TIER_MASTER).
 * Relegation: bottom [RELEGATION_ZONE_SIZE] ranks (only when tierOrder > TIER_BRONZE).
 */
private fun resolveZone(
    rank: Int,
    totalCount: Int,
    hasPromotion: Boolean,
    hasRelegation: Boolean
): StandingZone {
    if (hasPromotion && rank <= PROMOTION_ZONE_SIZE) return StandingZone.Promotion
    if (hasRelegation && totalCount > 0 && rank > totalCount - RELEGATION_ZONE_SIZE) return StandingZone.Relegation
    return StandingZone.None
}

enum class StandingZone { Promotion, Relegation, None }

@Composable
fun TierBadge(tierName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tierName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ZoneLegend(hasPromotion: Boolean, hasRelegation: Boolean) {
    if (!hasPromotion && !hasRelegation) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasPromotion) {
            LegendChip(
                color = Color(0xFF2E7D32),
                label = "Promotion zone",
                modifier = Modifier.weight(1f)
            )
        }
        if (hasRelegation) {
            LegendChip(
                color = Color(0xFFB71C1C),
                label = "Relegation zone",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun LegendChip(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LeagueStandingRow(
    standing: LeagueStandingDto,
    isCurrentUser: Boolean,
    zone: StandingZone
) {
    val zoneBackgroundAlpha = 0.15f
    val backgroundColor = when {
        isCurrentUser -> MaterialTheme.colorScheme.primaryContainer
        zone == StandingZone.Promotion -> Color(0xFF2E7D32).copy(alpha = zoneBackgroundAlpha)
        zone == StandingZone.Relegation -> Color(0xFFB71C1C).copy(alpha = zoneBackgroundAlpha)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val zoneAccentColor = when (zone) {
        StandingZone.Promotion -> Color(0xFF2E7D32)
        StandingZone.Relegation -> Color(0xFFB71C1C)
        StandingZone.None -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Zone accent stripe
            if (zone != StandingZone.None) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(zoneAccentColor, shape = RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            // Rank
            Text(
                text = "#${standing.rank}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp),
                color = if (isCurrentUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Username
            Text(
                text = standing.username,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.weight(1f),
                color = if (isCurrentUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            // Score
            Text(
                text = standing.weeklyScore.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isCurrentUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}
