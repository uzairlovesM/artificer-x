package com.waheed.artificerx.ui.screens.memory

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.workspace.MemoryEntity
import com.waheed.artificerx.data.workspace.MemoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemoryCenterViewModel @Inject constructor(private val repo: MemoryRepository) : ViewModel() {
    private val _items = MutableStateFlow<List<MemoryEntity>>(emptyList())
    val items = _items.asStateFlow()
    init { refresh() }
    fun refresh() { viewModelScope.launch { _items.value = repo.list("global") } }
    fun delete(item: MemoryEntity) { viewModelScope.launch { repo.delete(item.namespace, item.key); refresh() } }
}

@Composable
fun MemoryCenterScreen(onBack: () -> Unit, vm: MemoryCenterViewModel = hiltViewModel()) {
    val items by vm.items.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(items, query) { items.filter { query.isBlank() || it.key.contains(query, true) || it.value.contains(query, true) } }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }; Text("Memory Vault", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp)) }
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search persistent memory…") })
        Text("${filtered.size} local memories")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) { Text(item.key, style = MaterialTheme.typography.titleSmall); Text(item.value, style = MaterialTheme.typography.bodySmall) }
                    IconButton(onClick = { vm.delete(item) }) { Icon(Icons.Filled.Delete, "Delete memory") }
                }
            }
        }
    }
}
