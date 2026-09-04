package com.waheed.artificerx.ui.screens.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.diagnostics.FeatureAuditItem
import com.waheed.artificerx.core.diagnostics.FeatureWiringAudit
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog

@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    var items by remember { mutableStateOf(FeatureWiringAudit.run()) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("System Diagnostics") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, actions = { IconButton(onClick = { items = FeatureWiringAudit.run() }) { Icon(Icons.Filled.Refresh, "Re-run") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Runtime capability snapshot", style = MaterialTheme.typography.titleLarge)
                    Text("Tools: ${ToolRegistry.ALL_TOOLS.size}")
                    Text("Built-in plugins: ${BuiltinPluginCatalog.plugins.size}")
                    Text("Expected feature contracts: ${items.size}")
                } }
            }
            items(items, key = { it.id }) { item -> AuditRow(item) }
        }
    }
}

@Composable private fun AuditRow(item: FeatureAuditItem) {
    Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (item.missingCapabilities.isEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Warning, null)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text("Expected: ${item.expectedCapabilities.joinToString { it.name }}", style = MaterialTheme.typography.bodySmall)
            if (item.missingCapabilities.isNotEmpty()) Text("Missing: ${item.missingCapabilities.joinToString { it.name }}", color = MaterialTheme.colorScheme.error)
        }
    } }
}
