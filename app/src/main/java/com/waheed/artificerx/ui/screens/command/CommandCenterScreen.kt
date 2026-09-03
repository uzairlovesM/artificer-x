package com.waheed.artificerx.ui.screens.command

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.architecture.CapabilityGraph
import com.waheed.artificerx.core.insights.WorkspaceInsights
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog

@Composable
fun CommandCenterScreen(
    onBack: () -> Unit,
    onPlugins: () -> Unit,
    onTools: () -> Unit,
    onArtifacts: () -> Unit,
    onMemory: () -> Unit,
    onWorkflow: () -> Unit,
    onSecurity: () -> Unit,
    onSearch: () -> Unit,
    onDiagnostics: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onArtStudio: () -> Unit = {},
    onAgentWorkbench: () -> Unit = {},
    onPermissions: () -> Unit = {},
    onWorkspaceFiles: () -> Unit = {},
    onExtremeControl: () -> Unit = {},
) {
    val snapshot = remember { WorkspaceInsights.snapshot() }
    val reports = remember { CapabilityGraph.inspect() }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) {
                Text("ARTIFICER-X Command Center", style = MaterialTheme.typography.titleLarge)
                Text("Runtime topology, feature health and power tools", style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("System score ${snapshot.wiringScore}%", style = MaterialTheme.typography.headlineSmall)
                        Text("${snapshot.pluginCount} plugin descriptors • ${snapshot.toolCount} registered tools • ${snapshot.healthyFeatures}/${snapshot.totalFeatures} feature contracts healthy")
                    }
                }
            }
            item { Text("Feature contract matrix", style = MaterialTheme.typography.titleMedium) }
            items(reports) { report ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(report.node.title)
                        Text(if (report.healthy) "HEALTHY" else "AT RISK", color = if (report.healthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                        if (report.missingCategories.isNotEmpty()) Text("Missing families: ${report.missingCategories.joinToString()}", style = MaterialTheme.typography.bodySmall)
                        if (report.missingTools.isNotEmpty()) Text("Missing tools: ${report.missingTools.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Plugins", Icons.Filled.Extension, onPlugins)
            ActionButton("Tools", Icons.Filled.Build, onTools)
            ActionButton("Files", Icons.Filled.Folder, onArtifacts)
            ActionButton("Memory", Icons.Filled.Memory, onMemory)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton("Workflows", Icons.Filled.AutoMode, onWorkflow)
            ActionButton("Security", Icons.Filled.Security, onSecurity)
            ActionButton("Search", Icons.Filled.Search, onSearch)
            ActionButton("Diagnostics", Icons.Filled.BugReport, onDiagnostics)
            ActionButton("Export", Icons.Filled.Folder, onExport)
            ActionButton("Import", Icons.Filled.Folder, onImport)
            ActionButton("Art", Icons.Filled.Brush, onArtStudio)
            ActionButton("AI", Icons.Filled.AutoMode, onAgentWorkbench)
            ActionButton("Access", Icons.Filled.Security, onPermissions)
            ActionButton("Tree", Icons.Filled.Folder, onWorkspaceFiles)
            ActionButton("Extreme", Icons.Filled.AutoMode, onExtremeControl)
        }
    }
}

@Composable
private fun ActionButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.weight(1f)) { Icon(icon, null); Text(label, modifier = Modifier.padding(start = 4.dp)) }
}
