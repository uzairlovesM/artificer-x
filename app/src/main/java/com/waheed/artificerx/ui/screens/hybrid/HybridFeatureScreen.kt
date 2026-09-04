package com.waheed.artificerx.ui.screens.hybrid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.diagnostics.FeatureAuditItem
import com.waheed.artificerx.core.diagnostics.FeatureWiringAudit
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog
import com.waheed.artificerx.core.runtime.RuntimeToolCatalog
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.QualityFail
import com.waheed.artificerx.ui.theme.QualityPass
import com.waheed.artificerx.ui.theme.QualityWarn
import com.waheed.artificerx.ui.theme.glassSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridFeatureScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onOpenPlugins: (() -> Unit)? = null,
    onOpenToolUniverse: (() -> Unit)? = null,
    onOpenArtifactHub: (() -> Unit)? = null,
) {
    var audit by remember { mutableStateOf(FeatureWiringAudit.run()) }
    val plugins = BuiltinPluginCatalog.plugins
    val healthy = audit.count { it.status.name == "HEALTHY" }
    val score = if (audit.isEmpty()) 100 else (healthy * 100 / audit.size)
    val runtimeCount = RuntimeToolCatalog.definitions().size
    val featureAccent = when {
        title.contains("art", true) || title.contains("canvas", true) -> Icons.Filled.AutoAwesome
        title.contains("security", true) || title.contains("permission", true) -> Icons.Filled.Security
        title.contains("plugin", true) -> Icons.Filled.Extension
        title.contains("project", true) || title.contains("file", true) -> Icons.Filled.Folder
        else -> Icons.Filled.Memory
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(ArtificerXGradients.backgroundWash).padding(18.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(featureAccent, null, tint = GoldPrimary, modifier = Modifier.size(28.dp))
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(title, style = MaterialTheme.typography.headlineSmall)
                                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                AssistChip(onClick = {}, label = { Text("LIVE") }, leadingIcon = { Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp)) })
                            }
                            LinearProgressIndicator(progress = { score / 100f }, modifier = Modifier.fillMaxWidth())
                            Text("Capability health $score% • ${ToolRegistry.ALL_TOOLS.size} agent capabilities • $runtimeCount persistent runtime extensions", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { audit = FeatureWiringAudit.run() }) {
                                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp)); Text("Refresh")
                                }
                                onOpenToolUniverse?.let { OutlinedButton(onClick = it) { Icon(Icons.Filled.Terminal, null, modifier = Modifier.size(16.dp)); Text("Capabilities") } }
                            }
                        }
                    }
                }
            }

            item {
                Text("Runtime health", style = MaterialTheme.typography.titleLarge)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricCard("Healthy", healthy.toString(), QualityPass, Modifier.weight(1f))
                    MetricCard("At risk", (audit.size - healthy).toString(), QualityWarn, Modifier.weight(1f))
                    MetricCard("Plugins", plugins.size.toString(), GoldPrimary, Modifier.weight(1f))
                }
            }

            item { Text("Feature contract matrix", style = MaterialTheme.typography.titleLarge) }
            items(audit, key = { it.title }) { item -> AuditCard(item) }

            item { Text("Connected surfaces", style = MaterialTheme.typography.titleLarge) }
            items(plugins.take(24), key = { it.id }) { plugin ->
                Card(Modifier.fillMaxWidth().glassSurface()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Extension, null, tint = GoldPrimary)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(plugin.name, style = MaterialTheme.typography.titleSmall)
                            Text(plugin.category.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
                            Text(plugin.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.CheckCircle, null, tint = QualityPass)
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onOpenPlugins?.let { OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Extension, null); Text("Plugins") } }
                    onOpenArtifactHub?.let { OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Folder, null); Text("Artifacts") } }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, accent: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
        }
    }
}

@Composable
private fun AuditCard(item: FeatureAuditItem) {
    val healthy = item.status.name == "HEALTHY"
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (healthy) Icons.Filled.CheckCircle else Icons.Filled.Warning, null, tint = if (healthy) QualityPass else QualityFail)
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(if (healthy) "Verified capability surface" else "Capability needs attention", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (item.missingCapabilities.isEmpty()) {
                Text("All expected capability families are present and discoverable.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Missing: ${item.missingCapabilities.joinToString { it.name }}", color = QualityFail, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
