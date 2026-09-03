package com.waheed.artificerx.ui.screens.hybrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.diagnostics.FeatureAuditItem
import com.waheed.artificerx.core.diagnostics.FeatureWiringAudit
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog

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
    Scaffold(
        topBar = { SmallTopAppBar(title = { Text(title) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Memory, null)
                            Text("  $subtitle", style = MaterialTheme.typography.titleMedium)
                        }
                        Text("Real tool registry: ${ToolRegistry.ALL_TOOLS.size} tools • Built-in plugins: ${plugins.size}")
                        LinearProgressIndicator(progress = { (ToolRegistry.ALL_TOOLS.size / 1024f).coerceAtMost(1f) }, Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { audit = FeatureWiringAudit.run() }) { Icon(Icons.Filled.Refresh, null); Text(" Re-audit") }
                            onOpenPlugins?.let { callback -> OutlinedButton(onClick = callback) { Icon(Icons.Filled.Extension, null); Text(" Plugins") } }
                            onOpenToolUniverse?.let { callback -> OutlinedButton(onClick = callback) { Icon(Icons.Filled.Terminal, null); Text(" Tools") } }
                            onOpenArtifactHub?.let { callback -> OutlinedButton(onClick = callback) { Icon(Icons.Filled.PlayArrow, null); Text(" Artifacts") } }
                        }
                    }
                }
            }
            item { Text("Feature wiring matrix", style = MaterialTheme.typography.titleLarge) }
            items(audit) { item -> AuditCard(item) }
            item { Text("Capability surfaces", style = MaterialTheme.typography.titleLarge) }
            items(plugins.take(36)) { plugin ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Build, null)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(plugin.name, style = MaterialTheme.typography.titleSmall)
                            Text(plugin.category.name.replace('_', ' '), style = MaterialTheme.typography.labelSmall)
                            Text(plugin.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Filled.CheckCircle, null)
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@Composable
private fun AuditCard(item: FeatureAuditItem) {
    val healthy = item.status.name == "HEALTHY"
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (healthy) Icons.Filled.CheckCircle else Icons.Filled.Warning, null)
                Text("  ${item.title}", style = MaterialTheme.typography.titleMedium)
            }
            Text("Expected: ${item.expectedCapabilities.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
            if (item.missingCapabilities.isEmpty()) {
                Text("All expected capability families are present and discoverable.", style = MaterialTheme.typography.bodySmall)
            } else {
                Text("Missing: ${item.missingCapabilities.joinToString { it.name }}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
