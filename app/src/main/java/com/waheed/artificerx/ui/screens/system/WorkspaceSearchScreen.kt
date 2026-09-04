package com.waheed.artificerx.ui.screens.system

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.storage.WorkspaceIndex
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkspaceSearchViewModel @Inject constructor(private val index: WorkspaceIndex): ViewModel() {
    val query = MutableStateFlow("")
    private val _results = MutableStateFlow<List<WorkspaceIndex.Entry>>(emptyList())
    val results = _results.asStateFlow()
    fun search(q: String) { query.value=q; viewModelScope.launch { _results.value=index.scan(q) } }
}

@Composable fun WorkspaceSearchScreen(onBack:()->Unit, vm: WorkspaceSearchViewModel = hiltViewModel()) {
    val results by vm.results.collectAsState()
    var query by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick=onBack){ Icon(Icons.Filled.ArrowBack,null) }; Text("Workspace Search", style=MaterialTheme.typography.headlineSmall, modifier=Modifier.padding(top=10.dp)) }
        OutlinedTextField(query, { query=it; vm.search(it) }, Modifier.fillMaxWidth(), singleLine=true, label={Text("Search files, projects, models, plugins…")})
        LazyColumn(verticalArrangement=Arrangement.spacedBy(6.dp)) { items(results, key={it.path}) { e -> Card(Modifier.fillMaxWidth()){ Column(Modifier.padding(12.dp)){ Text(e.relativePath); Text("${e.sizeBytes} bytes • ${e.extension.ifBlank { "file" }}", style=MaterialTheme.typography.bodySmall) } } } }
    }
}
