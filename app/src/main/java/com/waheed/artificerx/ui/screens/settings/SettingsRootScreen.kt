package com.waheed.artificerx.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.PurpleAccent
import com.waheed.artificerx.ui.theme.glassSurface

/**
 * Root settings menu. Every entry here maps directly to a spec section:
 * AI Providers (195-199), Quality Budget (Section 84 "quality vs speed"
 * tradeoff the user controls), Cost/Usage Visibility (Section 210),
 * Storage (Section 27 project/version storage), Backup & Restore
 * (Section 139), Accessibility (Section 208), About.
 */
@Composable
fun SettingsRootScreen(
    onBack: () -> Unit,
    onOpenAiProviders: () -> Unit,
    onOpenLocalModel: () -> Unit,
    onOpenQualityBudget: () -> Unit,
    onOpenCostVisibility: () -> Unit,
    onOpenStorage: () -> Unit,
    onOpenBackupRestore: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenAbout: () -> Unit,
) {
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
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SettingsRow(
                    icon = Icons.Filled.Key,
                    title = "AI Providers",
                    subtitle = "Manage Groq, OpenRouter, Cloudflare & custom endpoints",
                    onClick = onOpenAiProviders,
                )
                SettingsRow(
                    icon = Icons.Filled.Memory,
                    title = "Local Model",
                    subtitle = "Run your own GGUF model offline — with optional mmproj vision",
                    onClick = onOpenLocalModel,
                )
                SettingsRow(
                    icon = Icons.Filled.Speed,
                    title = "Quality Budget",
                    subtitle = "Balance speed vs. self-correction depth",
                    onClick = onOpenQualityBudget,
                )
                SettingsRow(
                    icon = Icons.Filled.AccountBalanceWallet,
                    title = "Cost & Usage Visibility",
                    subtitle = "Track free-tier quota usage across providers",
                    onClick = onOpenCostVisibility,
                )
                SettingsRow(
                    icon = Icons.Filled.Storage,
                    title = "Storage",
                    subtitle = "Manage saved projects and version history",
                    onClick = onOpenStorage,
                )
                SettingsRow(
                    icon = Icons.Filled.Backup,
                    title = "Backup & Restore",
                    subtitle = "Export or import your projects and settings",
                    onClick = onOpenBackupRestore,
                )
                SettingsRow(
                    icon = Icons.Filled.Accessibility,
                    title = "Accessibility",
                    subtitle = "Content descriptions, contrast, text size",
                    onClick = onOpenAccessibility,
                )
                SettingsRow(
                    icon = Icons.Filled.Info,
                    title = "About ARTIFICER-X",
                    subtitle = "Version, licenses, build info",
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .glassSurface()
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .background(PurpleAccent.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
