package com.waheed.artificerx.ui.screens.hub

import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@Composable
fun ArtifactHubScreen(onBack: () -> Unit, viewModel: ArtifactHubViewModel = hiltViewModel()) {
    val artifacts by viewModel.artifacts.collectAsState()
    val context = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(title = { Text("Artifact Hub") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("${artifacts.size} persisted artifact(s)", style = MaterialTheme.typography.titleMedium)
            Text("Database-backed index. Files and metadata stay synchronized.", style = MaterialTheme.typography.bodySmall)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                items(artifacts, key = { it.id }) { artifact ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Description, null)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(artifact.name, style = MaterialTheme.typography.titleSmall)
                                Text("${artifact.sizeBytes} bytes • ${artifact.mimeType}", style = MaterialTheme.typography.bodySmall)
                            }
                            val file = File(artifact.path)
                            if (file.isFile) {
                                val uri = runCatching { FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file) }.getOrNull()
                                if (uri != null) IconButton(onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).setType(artifact.mimeType).putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(Intent.createChooser(intent, "Share artifact").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                }) { Icon(Icons.Filled.Share, "Share") }
                            }
                            IconButton(onClick = { viewModel.delete(artifact.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                        }
                    }
                }
            }
        }
    }
}

