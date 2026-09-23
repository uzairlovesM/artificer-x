package com.waheed.artificerx.ui.screens.system

import android.app.ActivityManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ObservatoryEntryPoint { fun fileSystem(): WorkspaceFileSystem }

@Composable
fun SystemObservatoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    if (activity == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("System Observatory needs an Android activity host.")
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        return
    }
    val fs = remember(activity) { EntryPointAccessors.fromActivity(activity, ObservatoryEntryPoint::class.java).fileSystem() }
    var refreshNonce by remember { mutableStateOf(0L) }
    var memoryText by remember { mutableStateOf("Unavailable") }
    var workspaceText by remember { mutableStateOf("Unavailable") }
    var directoryCounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val nativeStatus = remember {
        runCatching {
            System.loadLibrary("artificerx_native")
            "JNI loaded"
        }.getOrDefault("Unavailable")
    }
    val dirs = remember(fs) { listOf("works" to fs.roots.works, "cache" to fs.roots.cache, "system" to fs.roots.system, "plugins" to fs.roots.plugins, "models" to fs.roots.models, "exports" to fs.roots.exports, "imports" to fs.roots.imports, "logs" to fs.roots.logs, "temp" to fs.roots.temp, "backups" to fs.roots.backups, "autosave" to fs.roots.autosave, "projects" to fs.roots.projects) }
    LaunchedEffect(fs, refreshNonce) {
        val snapshot = withContext(Dispatchers.IO) {
            val reportedMemory = context.getSystemService(ActivityManager::class.java)?.let { manager ->
                ActivityManager.MemoryInfo().also(manager::getMemoryInfo)?.availMem?.div(1048576)?.let { "$it MiB" }
            } ?: "Unavailable"
            val counts = dirs.associate { (name, file) ->
                name to runCatching { "${fs.listFiles(file, false).size} files" }.getOrDefault("Unavailable")
            }
            reportedMemory to counts
        }
        memoryText = snapshot.first
        directoryCounts = snapshot.second
        workspaceText = withContext(Dispatchers.IO) { runCatching { "${fs.usageBytes() / 1024} KiB" }.getOrDefault("Unavailable") }
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { WorkspaceTopBar("System Observatory", "Runtime, workspace and capability telemetry", onBack) }
        item { OutlinedButton(onClick = { refreshNonce = System.currentTimeMillis() }) { Text("Refresh telemetry") } }
        item { MetricCard("Tools", ToolRegistry.ALL_TOOLS.size.toString(), "real agent capabilities") }
        item { MetricCard("Native raster", nativeStatus, "C++ analysis hot path") }
        item { MetricCard("Plugins", BuiltinPluginCatalog.plugins.size.toString(), "built-in plugin descriptors") }
        item { MetricCard("Available RAM", memoryText, "system reported") }
        item { MetricCard("Workspace", workspaceText, fs.roots.root.absolutePath) }
        item { Text("Managed paths", style = androidx.compose.material3.MaterialTheme.typography.titleMedium) }
        items(dirs) { (name, file) ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(name)
                        Text(file.absolutePath, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                    Text(directoryCounts[name] ?: "Unavailable")
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, detail: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
            Text(value, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            Text(detail, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
    }
}
