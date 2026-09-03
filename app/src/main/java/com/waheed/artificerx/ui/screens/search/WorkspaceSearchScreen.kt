package com.waheed.artificerx.ui.screens.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.search.WorkspaceSearch
import com.waheed.artificerx.core.search.WorkspaceSearchResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class UniversalSearchViewModel @Inject constructor(private val search: WorkspaceSearch) : ViewModel() {
    private val query = MutableStateFlow("")
    @OptIn(FlowPreview::class)
    val results: StateFlow<List<WorkspaceSearchResult>> = query.debounce(180).flatMapLatest { q -> flow { emit(search.search(q)) } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun setQuery(value: String) { query.value = value }
}

@Composable
fun UniversalSearchScreen(onBack: () -> Unit, vm: UniversalSearchViewModel = hiltViewModel()) {
    var query by remember { mutableStateOf("") }
    val results by vm.results.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }; Text("Universal Search", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp)) }
        OutlinedTextField(query, { query = it; vm.setQuery(it) }, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Chats, messages, artifacts…") })
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(results, key = { it.kind + it.id }) { r -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(r.title); Text("${r.kind} • ${r.subtitle}", style = MaterialTheme.typography.bodySmall) } } } }
    }
}
