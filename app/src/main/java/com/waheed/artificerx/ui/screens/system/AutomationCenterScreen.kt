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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.automation.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutomationCenterViewModel @Inject constructor(private val repo: AutomationRepository, private val engine: AutomationEngine, private val scheduler: AutomationScheduler): ViewModel() {
    private val _rules = MutableStateFlow<List<AutomationRule>>(emptyList()); val rules = _rules.asStateFlow()
    private val _logs = MutableStateFlow<List<String>>(emptyList()); val logs = _logs.asStateFlow()
    init { refresh(); scheduler.scheduleDaily() }
    fun refresh() = viewModelScope.launch { _rules.value = repo.list() }
    fun run(rule: AutomationRule) = viewModelScope.launch { _logs.value = listOf("${rule.name}: ${engine.run(rule)}") + _logs.value.take(9) }
    fun toggle(rule: AutomationRule) = viewModelScope.launch { repo.save(_rules.value.map { if (it.id == rule.id) it.copy(enabled = !it.enabled) else it }); refresh() }
}

@Composable
fun AutomationCenterScreen(onBack: () -> Unit, vm: AutomationCenterViewModel = hiltViewModel()) {
    val rules by vm.rules.collectAsState()
    val logs by vm.logs.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) }; Text("Automation Center", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top=10.dp)) }
        Text("Real local automations run through WorkManager and the workspace services.", style = MaterialTheme.typography.bodyMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(rules, key = { it.id }) { rule ->
                androidx.compose.material3.Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp)) {
                        Column(Modifier.weight(1f)) { Text(rule.name); Text("${rule.trigger} • ${rule.action}", style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { vm.toggle(rule) }) { Text(if (rule.enabled) "Enabled" else "Disabled") }
                        IconButton(onClick = { vm.run(rule) }) { Icon(Icons.Filled.PlayArrow, "Run") }
                    }
                }
            }
            item { Text("Recent runs", style = MaterialTheme.typography.titleMedium) }
            items(logs) { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

