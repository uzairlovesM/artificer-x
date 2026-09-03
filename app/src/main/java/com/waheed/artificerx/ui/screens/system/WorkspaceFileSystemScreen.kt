package com.waheed.artificerx.ui.screens.system

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
    val context=androidx.compose.ui.platform.LocalContext.current
    val activity=context as android.app.Activity
    val ep=EntryPointAccessors.fromActivity(activity,WorkspaceFsEntryPoint::class.java)
    val fs=ep.fs()
    val roots=listOf("works" to fs.roots.works,"cache" to fs.roots.cache,"system" to fs.roots.system,"plugins" to fs.roots.plugins,"models" to fs.roots.models,"exports" to fs.roots.exports,"imports" to fs.roots.imports,"logs" to fs.roots.logs,"temp" to fs.roots.temp,"thumbnails" to fs.roots.thumbnails,"backups" to fs.roots.backups,"autosave" to fs.roots.autosave,"projects" to fs.roots.projects,"recipes" to fs.roots.recipes)
    Scaffold(topBar={WorkspaceTopBar("Workspace Files","Inspect the real on-device Artificer-X data tree",onBack)}){pad->
        Column(Modifier.fillMaxSize().padding(pad).padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Button(onClick={fs.ensureReady()}){Text("Initialize / refresh")}; Text("${fs.roots.root.absolutePath}\nTotal ${fs.usageBytes()} bytes")
            LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)){items(roots){(n,f)->Column{Text(n);Text(f.absolutePath);Text("${f.length()} bytes • ${f.walkTopDown().count{it.isFile}} files",style=androidx.compose.material3.MaterialTheme.typography.bodySmall)}}}
        }
    }
}
