package com.waheed.artificerx.ui.screens.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(ActivityComponent::class)
interface WorkspaceFsEntryPoint { fun fs(): WorkspaceFileSystem }

data class WorkspaceRootSnapshot(
    val name: String,
    val path: String,
    val bytes: Long,
    val files: Long,
)

@Composable
fun WorkspaceFileSystemScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity
    if (activity == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Workspace Files needs an Android activity host.")
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        return
    }
    val ep = remember(activity) { EntryPointAccessors.fromActivity(activity, WorkspaceFsEntryPoint::class.java) }
    val fs = ep.fs()
    val roots = remember(fs) { listOf("works" to fs.roots.works, "cache" to fs.roots.cache, "system" to fs.roots.system, "plugins" to fs.roots.plugins, "models" to fs.roots.models, "exports" to fs.roots.exports, "imports" to fs.roots.imports, "logs" to fs.roots.logs, "temp" to fs.roots.temp, "thumbnails" to fs.roots.thumbnails, "backups" to fs.roots.backups, "autosave" to fs.roots.autosave, "projects" to fs.roots.projects, "recipes" to fs.roots.recipes) }
    var refreshNonce by remember { mutableStateOf(0L) }
    var totalBytes by remember { mutableStateOf<Long?>(null) }
    var snapshots by remember { mutableStateOf<List<WorkspaceRootSnapshot>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(fs, refreshNonce) {
        val result = runCatching {
            withContext(Dispatchers.IO) {
                val rootSnapshots = roots.map { (name, file) ->
                    WorkspaceRootSnapshot(
                        name = name,
                        path = file.absolutePath,
                        bytes = file.length(),
                        files = file.walkTopDown().count { it.isFile },
                    )
                }
                rootSnapshots to fs.usageBytes()
            }
        }
        result.onSuccess { (rootSnapshots, usage) ->
            snapshots = rootSnapshots
            totalBytes = usage
            errorMessage = null
        }.onFailure {
            errorMessage = it.message ?: "Could not inspect workspace."
        }
    }

    Scaffold(topBar = { WorkspaceTopBar("Workspace Files", "Inspect the real on-device Artificer-X data tree", onBack) }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                runCatching { fs.ensureReady() }
                    .onSuccess { refreshNonce = System.currentTimeMillis() }
                    .onFailure { errorMessage = it.message ?: "Could not initialize workspace." }
            }) { Text("Initialize / refresh") }
            Text("${fs.roots.root.absolutePath}
Total ${totalBytes?.let { "$it bytes" } ?: "calculating…"}")
            errorMessage?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(snapshots, key = { it.path }) { root ->
                    Column {
                        Text(root.name)
                        Text(root.path)
                        Text("${root.bytes} bytes • ${root.files} files", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
