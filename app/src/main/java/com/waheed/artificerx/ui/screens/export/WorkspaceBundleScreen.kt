package com.waheed.artificerx.ui.screens.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.importexport.WorkspaceBundleService
import com.waheed.artificerx.data.local.datastore.ChatSessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceBundleViewModel @Inject constructor(
    private val service: WorkspaceBundleService,
    private val sessions: ChatSessionDataStore,
) : ViewModel() {
    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status.asStateFlow()

    fun export() {
        viewModelScope.launch {
            val thread = sessions.getActiveThreadId()
            if (thread.isNullOrBlank()) {
                _status.value = "No active conversation. Open AI Chat first."
                return@launch
            }
            _status.value = "Building portable workspace bundle…"
            runCatching { service.exportThread(thread) }
                .onSuccess { ref -> _status.value = "Created ${ref.name} • ${ref.sizeBytes} bytes • artifact ${ref.id}" }
                .onFailure { error -> _status.value = "Export failed: ${error.message ?: "unknown error"}" }
        }
    }
}

@androidx.compose.runtime.Composable
fun WorkspaceBundleScreen(onBack: () -> Unit, viewModel: WorkspaceBundleViewModel = hiltViewModel()) {
    val status by viewModel.status.collectAsState()
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Workspace Export") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
        )
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Archive, null)
                        Text("Portable workspace bundle", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("Packages the active chat, redacted memory snapshot, metadata and readable generated artifacts into one validated ZIP.")
                    Text(status, color = if (status.startsWith("Export failed")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Button(onClick = viewModel::export, modifier = Modifier.fillMaxWidth()) { Text("Export active workspace") }
                }
            }
        }
    }
}

