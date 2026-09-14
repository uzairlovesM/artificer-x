package com.waheed.artificerx.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.search.WorkspaceSearch
import com.waheed.artificerx.core.search.WorkspaceSearchResult
import com.waheed.artificerx.data.repository.ChatWorkspaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UniversalSearchViewModel
    @Inject
    constructor(
        private val search: WorkspaceSearch,
        private val workspaceRepository: ChatWorkspaceRepository,
    ) : ViewModel() {
        private val query = MutableStateFlow("")

        @OptIn(FlowPreview::class)
        val results: StateFlow<List<WorkspaceSearchResult>> =
            query
                .debounce(180)
                .flatMapLatest { q -> flow { emit(search.search(q)) } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        fun setQuery(value: String) {
            query.value = value
        }

        /** "chat" results already carry the threadId directly. "message"
         *  results carry a message row id and need one lookup to find the
         *  thread that message lives in — a message can't be opened on its
         *  own, only the conversation containing it. */
        fun openConversationResult(result: WorkspaceSearchResult, onOpenChat: (String) -> Unit) {
            viewModelScope.launch {
                val threadId =
                    when (result.kind) {
                        "chat" -> result.id
                        "message" -> workspaceRepository.getMessageThreadId(result.id)
                        else -> null
                    }
                if (!threadId.isNullOrBlank()) onOpenChat(threadId)
            }
        }

        suspend fun getArtifactPathAndMime(artifactId: String): Pair<String, String>? =
            workspaceRepository.getArtifact(artifactId)?.let { it.path to it.mimeType }
    }

@Composable
fun UniversalSearchScreen(
    onBack: () -> Unit,
    onOpenChat: (threadId: String) -> Unit = {},
    vm: UniversalSearchViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    val results by vm.results.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun openArtifact(artifactId: String) {
        coroutineScope.launch {
            val pathAndMime = vm.getArtifactPathAndMime(artifactId) ?: return@launch
            val (path, mime) = pathAndMime
            val file = java.io.File(path)
            if (!file.exists()) return@launch
            val uri =
                runCatching {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        context.packageName + ".fileprovider",
                        file,
                    )
                }.getOrNull() ?: return@launch
            runCatching {
                context.startActivity(
                    android.content.Intent(android.content.Intent.ACTION_VIEW)
                        .setDataAndType(uri, mime)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION),
                )
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
            Text("Universal Search", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp))
        }
        OutlinedTextField(
            query,
            { query = it; vm.setQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            placeholder = { Text("Chats, messages, artifacts…") },
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.kind + it.id }) { r ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        when (r.kind) {
                            "chat", "message" -> vm.openConversationResult(r, onOpenChat)
                            "artifact" -> openArtifact(r.id)
                        }
                    },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector =
                                when (r.kind) {
                                    "chat", "message" -> Icons.Filled.Chat
                                    "artifact" -> Icons.Filled.InsertDriveFile
                                    else -> Icons.Filled.Description
                                },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(r.title)
                            Text("${r.kind} • ${r.subtitle}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
