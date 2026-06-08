package com.wordsearch.ui.credits

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A single credited asset (music, art, SFX). [license] / [licenseUrl] satisfy the
 * attribution requirement of licenses like Creative Commons BY 4.0 — every CC BY asset
 * MUST appear here with author + license + source link, or the license is breached.
 */
data class Attribution(
    val title: String,
    val author: String,
    val source: String,
    val license: String,
    val licenseUrl: String,
    val sourceUrl: String? = null
)

/**
 * All third-party asset attributions shown in the in-app Credits screen.
 * Add an entry here whenever a new licensed asset (e.g. a CC BY music track) is bundled.
 */
val ATTRIBUTIONS: List<Attribution> = listOf(
    Attribution(
        title = "Journey To Ascend",
        author = "Kevin MacLeod",
        source = "incompetech.com",
        license = "Creative Commons: By Attribution 4.0",
        licenseUrl = "http://creativecommons.org/licenses/by/4.0/",
        sourceUrl = "https://incompetech.com"
    ),
    Attribution(
        title = "Fox Tale Waltz Part 1 Instrumental",
        author = "Kevin MacLeod",
        source = "incompetech.com",
        license = "Creative Commons: By Attribution 4.0",
        licenseUrl = "http://creativecommons.org/licenses/by/4.0/",
        sourceUrl = "https://incompetech.com"
    ),
    Attribution(
        title = "Half Mystery",
        author = "Kevin MacLeod",
        source = "incompetech.com",
        license = "Creative Commons: By Attribution 4.0",
        licenseUrl = "http://creativecommons.org/licenses/by/4.0/",
        sourceUrl = "https://incompetech.com"
    ),
    Attribution(
        title = "Whimsy Groove",
        author = "Kevin MacLeod",
        source = "incompetech.com",
        license = "Creative Commons: By Attribution 4.0",
        licenseUrl = "http://creativecommons.org/licenses/by/4.0/",
        sourceUrl = "https://incompetech.com"
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Credits") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Music",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "This game uses music licensed under Creative Commons. " +
                    "Attribution is provided below as required by each license.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            ATTRIBUTIONS.forEach { attribution ->
                AttributionCard(
                    attribution = attribution,
                    onOpenLicense = { url ->
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun AttributionCard(
    attribution: Attribution,
    onOpenLicense: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "“${attribution.title}”",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "by ${attribution.author} (${attribution.source})",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                attribution.license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = { onOpenLicense(attribution.licenseUrl) },
                    label = { Text("License") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                attribution.sourceUrl?.let { url ->
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = { onOpenLicense(url) },
                        label = { Text("Source") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}
