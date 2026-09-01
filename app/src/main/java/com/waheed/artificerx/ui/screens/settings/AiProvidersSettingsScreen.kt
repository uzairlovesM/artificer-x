package com.waheed.artificerx.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.domain.model.AiProviderConfig
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.PurpleAccent
import com.waheed.artificerx.ui.theme.QualityFail
import com.waheed.artificerx.ui.theme.QualityWarn
import com.waheed.artificerx.ui.theme.glassSurface

/**
 * Live-data provider management (Section 195-199, 210 Cost Visibility).
 * Each configured provider shows: enable/disable toggle, primary-brain
 * star, near-quota warning bar, and delete. Backed directly by
 * ProviderConfigRepository — this is real persisted state, not a mock.
 */
@Composable
fun AiProvidersSettingsScreen(
    onBack: () -> Unit,
    onAddProvider: () -> Unit,
    onOpenProviderDetail: (String) -> Unit,
    viewModel: AiProvidersSettingsViewModel = hiltViewModel(),
) {
    val providers by viewModel.providers.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddProvider,
                containerColor = GoldPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add provider")
            }
        },
    ) { scaffoldPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .background(ArtificerXGradients.backgroundWash),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    Text(
                        text = "AI Providers",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (providers.isEmpty()) {
                    EmptyProvidersState()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(providers, key = { it.id }) { provider ->
                            ProviderCard(
                                provider = provider,
                                onToggle = { viewModel.toggleEnabled(provider) },
                                onSetPrimary = { viewModel.setPrimary(provider) },
                                onDelete = { viewModel.removeProvider(provider) },
                                onClick = { onOpenProviderDetail(provider.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyProvidersState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No providers configured",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ProviderCard(
    provider: AiProviderConfig,
    onToggle: () -> Unit,
    onSetPrimary: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .glassSurface()
                .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSetPrimary, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = if (provider.isPrimary) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "Set as primary",
                    tint = if (provider.isPrimary) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = provider.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = provider.maskedKeyPreview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = provider.isEnabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(checkedTrackColor = GoldPrimary, checkedThumbColor = MaterialTheme.colorScheme.onPrimary),
            )
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove provider", tint = QualityFail, modifier = Modifier.size(18.dp))
            }
        }

        if (provider.knownDailyQuota != null) {
            Spacer(modifier = Modifier.padding(top = 10.dp))
            val progress = (provider.usageTodayCallCount.toFloat() / provider.knownDailyQuota).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = if (provider.isNearQuota) QualityWarn else PurpleAccent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(modifier = Modifier.padding(top = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (provider.isNearQuota) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = QualityWarn, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.padding(start = 4.dp))
                }
                Text(
                    text = "${provider.usageTodayCallCount} / ${provider.knownDailyQuota} calls today",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
