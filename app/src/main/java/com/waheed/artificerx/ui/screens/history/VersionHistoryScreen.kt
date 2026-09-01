package com.waheed.artificerx.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.data.local.db.ProjectVersionEntity
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.PurpleAccent
import com.waheed.artificerx.ui.theme.QualityFail
import com.waheed.artificerx.ui.theme.glassSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VersionHistoryScreen(
    projectId: String,
    onBack: () -> Unit,
    onRestoreComplete: () -> Unit,
    viewModel: VersionHistoryViewModel = hiltViewModel(),
) {
    val versions by viewModel.versionsFor(projectId).collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.restoreOutcome) {
        if (uiState.restoreOutcome is RestoreOutcome.Success) {
            onRestoreComplete()
            viewModel.clearOutcome()
        }
    }

    val pendingVersion = versions.firstOrNull { it.id == uiState.pendingRestoreVersionId }
    if (pendingVersion != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRestoreConfirmation,
            title = { Text("Restore this version?") },
            text = {
                Text(
                    "This will make '${pendingVersion.versionLabel}' the current state. Nothing is deleted — the current version stays in your history.",
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRestore(pendingVersion) }) {
                    Text("Restore", color = GoldPrimary, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelRestoreConfirmation) {
                    Text("Cancel")
                }
            },
        )
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(ArtificerXGradients.backgroundWash),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "Version History",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (versions.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(versions, key = { it.id }) { version ->
                        VersionRow(
                            version = version,
                            onRestore = { viewModel.requestRestore(version.id) },
                        )
                    }
                }
            }

            if (uiState.restoreOutcome is RestoreOutcome.Failure) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .glassSurface()
                            .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = (uiState.restoreOutcome as RestoreOutcome.Failure).message,
                        color = QualityFail,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.History, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.padding(top = 12.dp))
            Text(
                text = "No checkpoints yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Checkpoints are created automatically as you and the agent work",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VersionRow(
    version: ProjectVersionEntity,
    onRestore: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .glassSurface()
                .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(PurpleAccent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.History, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = version.versionLabel,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = "${formatTimestamp(version.createdAtEpochMillis)} · ${version.triggeredBy}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.Filled.Restore, contentDescription = "Restore this version", tint = GoldPrimary)
        }
    }
}

private fun formatTimestamp(epochMillis: Long): String {
    val formatter = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return formatter.format(Date(epochMillis))
}
