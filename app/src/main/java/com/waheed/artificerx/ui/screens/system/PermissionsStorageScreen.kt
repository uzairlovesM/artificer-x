package com.waheed.artificerx.ui.screens.system

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.waheed.artificerx.core.permissions.PermissionManager
import com.waheed.artificerx.core.storage.ExternalStorageGateway
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface PermissionScreenEntryPoint {
    fun fileSystem(): WorkspaceFileSystem
    fun externalStorageGateway(): ExternalStorageGateway
}

@Composable
fun PermissionsStorageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val ep = remember { EntryPointAccessors.fromActivity(activity, PermissionScreenEntryPoint::class.java) }
    val fs = ep.fileSystem(); val gateway = ep.externalStorageGateway()
    var stamp by remember { mutableStateOf(0L) }
    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { stamp = System.currentTimeMillis() }
    val treePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? -> uri?.let { gateway.persistTreePermission(it) } }
    val rows = PermissionManager.Capability.entries
    Scaffold(topBar={WorkspaceTopBar("Permissions & Storage","Runtime permissions, SAF access and local workspace paths",onBack)}) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            item { Text("Runtime access", style=androidx.compose.material3.MaterialTheme.typography.titleLarge) }
            items(rows) { capability ->
                val granted = PermissionManager.isGranted(context, capability)
                Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)){Text(capability.name.replace('_',' '));Text(if(granted)"Granted" else "Not granted", style=androidx.compose.material3.MaterialTheme.typography.bodySmall)}
                    if(!granted && capability.permissions.isNotEmpty()) Button(onClick={runtime.launch(capability.permissions.toTypedArray())}){Text("Grant")}
                }
            }
            item { HorizontalDivider() }
            item { Text("Workspace filesystem", style=androidx.compose.material3.MaterialTheme.typography.titleLarge) }
            item { Text("Root: ${fs.roots.root.absolutePath}") }
            items(listOf(
                "works" to fs.roots.works, "cache" to fs.roots.cache, "system" to fs.roots.system, "plugins" to fs.roots.plugins,
                "models" to fs.roots.models, "exports" to fs.roots.exports, "imports" to fs.roots.imports, "logs" to fs.roots.logs,
                "temp" to fs.roots.temp, "thumbnails" to fs.roots.thumbnails, "backups" to fs.roots.backups, "autosave" to fs.roots.autosave,
                "projects" to fs.roots.projects, "recipes" to fs.roots.recipes
            )) { (name,file) -> Text("$name  •  ${file.length()} bytes  •  ${file.listFiles()?.size ?: 0} children") }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={fs.ensureReady();stamp=System.currentTimeMillis()}){Text("Initialize")};OutlinedButton(onClick={fs.clearCache();stamp=System.currentTimeMillis()}){Text("Clear cache")}} }
            item { Text("External storage", style=androidx.compose.material3.MaterialTheme.typography.titleLarge) }
            item { Text("Use Android's Storage Access Framework for user-selected folders. MediaStore is used for app-published images/documents.") }
            item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={treePicker.launch(null)}){Text("Grant folder access")};OutlinedButton(onClick={context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))}){Text("App settings")}} }
            if (Build.VERSION.SDK_INT >= 30) item { OutlinedButton(onClick={runCatching { context.startActivity(PermissionManager.manageAllFilesIntent(context)) }} ){Text("Open all-files settings") } }
        }
    }
}
