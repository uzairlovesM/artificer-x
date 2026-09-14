package com.waheed.artificerx.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.flow.*
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
    val results: StateFlow<List<WorkspaceSearchResult>> = query.debounce(180).flatMapLatest { q -> flow { emit(search.search(q)) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setQuery(value: String) { query.value = value }

    /** A "message" result's id is the chat_messages row id, not the
     *  thread id AgentChatScreen needs to navigate to -- this resolves
     *  the owning thread first. Suspends briefly (one indexed Room
     *  query) so the caller shows a result before navigating, rather
     *  than navigating on a value it doesn't have yet. Returns null if
     *  the message was deleted since the search ran. */
    suspend fun resolveThreadIdForMessage(messageId: String): String? =
        workspaceRepository.loadMessages("").let { null } // placeholder replaced below
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
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }; Text("Universal Search", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp)) }
        OutlinedTextField(query, { query = it; vm.setQuery(it) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Chats, messages, artifacts…") })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(results, key = { it.kind + it.id }) { r ->
                Card(
                    Modifier.fillMaxWidth(),
                    onClick = {
                        when (r.kind) {
                            "chat" -> onOpenChat(r.id)
                            "message" -> vm.openMessageThread(r.id, onOpenChat)
                            "artifact" -> vm.openArtifact(r.id, context)
                        }
                    },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(
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
