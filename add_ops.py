from pathlib import Path
root=Path('/mnt/data/artificer_upgrade_work')
def write(rel,text):
 p=root/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)

write('app/src/main/java/com/waheed/artificerx/ui/screens/system/PermissionsStorageScreen.kt', r'''package com.waheed.artificerx.ui.screens.system

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
            if (Build.VERSION.SDK_INT >= 30) item { OutlinedButton(onClick={runCatching{context.startActivity(PermissionManager.manageAllFilesIntent(context))}}.getOrNull() as Unit){Text("Open all-files settings") } }
        }
    }
}
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/ai/AgentWorkbenchScreen.kt', r'''package com.waheed.artificerx.ui.screens.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.ui.components.SoftCard
import com.waheed.artificerx.ui.components.WorkspaceTopBar

@Composable
fun AgentWorkbenchScreen(onBack:()->Unit,onChat:()->Unit,onTools:()->Unit){
 var planning by remember{mutableStateOf(true)};var memory by remember{mutableStateOf(true)};var artifacts by remember{mutableStateOf(true)};var selfCorrect by remember{mutableStateOf(true)};var web by remember{mutableStateOf(false)};var budget by remember{mutableFloatStateOf(0.72f)};var maxTools by remember{mutableIntStateOf(32)}
 val confidence by animateFloatAsState(if(planning && memory && selfCorrect) .96f else .65f,label="confidence")
 Scaffold(topBar={WorkspaceTopBar("Agent Workbench","Claude-inspired reasoning workspace and execution controls",onBack)}){pad->
  Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
   SoftCard{Text("Reasoning profile",style=androidx.compose.material3.MaterialTheme.typography.titleLarge);Text(if(planning)"Plan → execute → inspect → self-correct → finalize" else "Direct execution mode");LinearProgressIndicator({confidence},Modifier.fillMaxWidth())}
   SoftCard{Setting("Planning loop","Build a plan before changing files",planning){planning=!planning};Setting("Memory","Recall persistent project/user memory",memory){memory=!memory};Setting("Artifact-first","Prefer files/ZIP/images over prose-only replies",artifacts){artifacts=!artifacts};Setting("Self-correction","Retry failed tools with revised arguments",selfCorrect){selfCorrect=!selfCorrect};Setting("Web tools","Allow web search/fetch capabilities",web){web=!web}}
   SoftCard{Text("Execution budget",style=androidx.compose.material3.MaterialTheme.typography.titleMedium);Slider(budget,{budget=it},valueRange=.1f..1f);Text("Tool budget ${((budget*100).toInt())}%");Slider(maxTools.toFloat(),{maxTools=it.toInt()},valueRange=4f..128f,steps=30);Text("Max tools per turn: $maxTools")}
   SoftCard{Text("Workspace actions",style=androidx.compose.material3.MaterialTheme.typography.titleMedium);Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick=onChat){Text("Open chat")};Button(onClick=onTools){Text("Inspect tools")}}}
   AnimatedVisibility(planning){Text("Plan checkpoints: intent → context → tool selection → execution → artifact validation → response")}
  }
 }
}

@Composable private fun Setting(title:String,subtitle:String,value:Boolean,onToggle:()->Unit){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)){Column(Modifier.weight(1f)){Text(title);Text(subtitle,style=androidx.compose.material3.MaterialTheme.typography.bodySmall)};Switch(value,onClick=onToggle)}}
''')

write('app/src/main/java/com/waheed/artificerx/ui/screens/system/WorkspaceFileSystemScreen.kt', r'''package com.waheed.artificerx.ui.screens.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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

@EntryPoint
@InstallIn(ActivityComponent::class)
interface WorkspaceFsEntryPoint { fun fs(): WorkspaceFileSystem }

@Composable
fun WorkspaceFileSystemScreen(onBack:()->Unit){
 val context=androidx.compose.ui.platform.LocalContext.current;val activity=context as android.app.Activity;val ep=remember{EntryPointAccessors.fromActivity(activity,WorkspaceFsEntryPoint::class.java)};val fs=ep.fs();var tick by remember{mutableStateOf(0)}
 Scaffold(topBar={WorkspaceTopBar("Workspace Files","Inspect the real on-device Artificer-X data tree",onBack)}){pad->Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={fs.ensureReady();tick++}){Text("Refresh")};Text("${fs.roots.root.absolutePath}\nTotal ${fs.usageBytes()} bytes");LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)){items(listOf("works" to fs.roots.works,"cache" to fs.roots.cache,"system" to fs.roots.system,"plugins" to fs.roots.plugins,"models" to fs.roots.models,"exports" to fs.roots.exports,"imports" to fs.roots.imports,"logs" to fs.roots.logs,"temp" to fs.roots.temp,"thumbnails" to fs.roots.thumbnails,"backups" to fs.roots.backups,"autosave" to fs.roots.autosave,"projects" to fs.roots.projects,"recipes" to fs.roots.recipes)){(n,f)->Column{Text(n);Text(f.absolutePath);Text("${f.length()} bytes • ${f.walkTopDown().count{it.isFile}} files",style=androidx.compose.material3.MaterialTheme.typography.bodySmall)}}}}}}
}
''')
