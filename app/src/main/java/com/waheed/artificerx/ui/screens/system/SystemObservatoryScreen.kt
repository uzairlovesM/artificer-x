package com.waheed.artificerx.ui.screens.system

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn

@EntryPoint
@InstallIn(ActivityComponent::class)
interface ObservatoryEntryPoint { fun fileSystem(): WorkspaceFileSystem }

@Composable
fun SystemObservatoryScreen(onBack: () -> Unit) {
    val context=LocalContext.current
    val activity=context as android.app.Activity
    val fs=remember { EntryPointAccessors.fromActivity(activity, ObservatoryEntryPoint::class.java).fileSystem() }
    val memory=remember { (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).run { ActivityManager.MemoryInfo().also{getMemoryInfo(it)} } }
    val dirs=remember { listOf("works" to fs.roots.works, "cache" to fs.roots.cache, "system" to fs.roots.system, "plugins" to fs.roots.plugins, "models" to fs.roots.models, "exports" to fs.roots.exports, "imports" to fs.roots.imports, "logs" to fs.roots.logs, "temp" to fs.roots.temp, "backups" to fs.roots.backups, "autosave" to fs.roots.autosave, "projects" to fs.roots.projects) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
        item { WorkspaceTopBar("System Observatory", "Runtime, workspace and capability telemetry", onBack) }
        item { MetricCard("Tools", ToolRegistry.ALL_TOOLS.size.toString(), "real agent capabilities") }
        item { MetricCard("Native raster", runCatching { System.loadLibrary("artificerx_native"); "JNI loaded" }.getOrDefault("Unavailable"), "C++ analysis hot path") }
        item { MetricCard("Plugins", BuiltinPluginCatalog.plugins.size.toString(), "built-in plugin descriptors") }
        item { MetricCard("Available RAM", "${(memory.availMem/1048576)} MiB", "system reported") }
        item { MetricCard("Workspace", "${fs.usageBytes()/1024} KiB", fs.roots.root.absolutePath) }
        item { Text("Managed paths", style=androidx.compose.material3.MaterialTheme.typography.titleMedium) }
        items(dirs) { (name,file) -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), horizontalArrangement=Arrangement.SpaceBetween) { Column(Modifier.weight(1f)){Text(name);Text(file.absolutePath, style=androidx.compose.material3.MaterialTheme.typography.bodySmall)}; Text("${fs.listFiles(file, false).size} files") } } }
    }
}

@Composable private fun MetricCard(label:String,value:String,detail:String){ Card(Modifier.fillMaxWidth()){ Column(Modifier.padding(14.dp)){Text(label,style=androidx.compose.material3.MaterialTheme.typography.labelLarge);Text(value,style=androidx.compose.material3.MaterialTheme.typography.headlineSmall);Text(detail,style=androidx.compose.material3.MaterialTheme.typography.bodySmall)} } }
