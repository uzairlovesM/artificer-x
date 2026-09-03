package com.waheed.artificerx.ui.screens.importexport

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.importexport.WorkspaceBundleImporter
import com.waheed.artificerx.data.local.datastore.ChatSessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importer: WorkspaceBundleImporter,
    private val sessions: ChatSessionDataStore,
) : ViewModel() {
    private val _status = MutableStateFlow("Choose an Artificer-X ZIP or a compatible workspace bundle.")
    val status: StateFlow<String> = _status.asStateFlow()

    fun importUri(uri: Uri) {
        viewModelScope.launch {
            val thread = sessions.getActiveThreadId()
            if (thread.isNullOrBlank()) {
                _status.value = "No active conversation. Open AI Chat first."
                return@launch
            }
            _status.value = "Importing bundle…"
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { importer.importIntoThread(thread, it).size }
                    ?: error("Could not open selected file")
            }.onSuccess { count -> _status.value = "Imported $count artifact(s) into the active workspace." }
                .onFailure { error -> _status.value = "Import failed: ${error.message ?: "unknown error"}" }
        }
    }
}

@Composable
fun WorkspaceImportScreen(onBack: () -> Unit, viewModel: WorkspaceImportViewModel = hiltViewModel()) {
    val status by viewModel.status.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let(viewModel::importUri) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Workspace Import") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Filled.FileOpen, null)
                Text("Secure bundle importer")
                Text("Imports files from a ZIP into the active artifact workspace. Unsafe paths and oversized archives are rejected.")
                Text(status)
                Button(onClick = { picker.launch("application/zip") }, modifier = Modifier.fillMaxWidth()) { Text("Choose ZIP") }
            } }
        }
    }
}
