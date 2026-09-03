package com.waheed.artificerx.ui.screens.plugins

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog
import com.waheed.artificerx.core.plugin.PluginManager
import com.waheed.artificerx.core.plugin.PluginLifecycleCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginCenterViewModel @Inject constructor(private val manager: PluginManager, private val lifecycle: PluginLifecycleCoordinator): ViewModel() {
    private val _installed = MutableStateFlow(emptySet<String>())
    val installed: StateFlow<Set<String>> = _installed
    init { viewModelScope.launch { manager.installed.collect { _installed.value = it.filter { r -> r.enabled }.map { r -> r.id }.toSet() } } }
    fun toggle(id: String) { viewModelScope.launch { if (id in _installed.value) manager.setEnabled(id, false) else lifecycle.installWithDependencies(id) } }
}

@Composable
fun PluginCenterScreen(onBack: () -> Unit, viewModel: PluginCenterViewModel = hiltViewModel()) {
    val installed by viewModel.installed.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
            Text("Plugin Center", style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(BuiltinPluginCatalog.plugins, key = { it.id }) { plugin ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(plugin.name, style = MaterialTheme.typography.titleMedium)
                            Text("${plugin.category.name} • ${plugin.version}", style = MaterialTheme.typography.labelMedium)
                            Text(plugin.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(onClick = { viewModel.toggle(plugin.id) }) { Text(if (plugin.id in installed) "Enabled" else "Install") }
                    }
                }
            }
        }
    }
}
