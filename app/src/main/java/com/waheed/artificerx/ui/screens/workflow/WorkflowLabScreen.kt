package com.waheed.artificerx.ui.screens.workflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.workflow.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WorkflowLabViewModel @Inject constructor(private val store: WorkflowStore, private val actionRunner: DefaultWorkflowActionRunner) : ViewModel() {
    private val _workflows = MutableStateFlow<List<WorkflowDefinition>>(emptyList())
    val workflows = _workflows.asStateFlow()
    private val _lastRun = MutableStateFlow<WorkflowRunResult?>(null)
    val lastRun = _lastRun.asStateFlow()
    init { viewModelScope.launch { store.workflows.collect { _workflows.value = it } } }
    fun save(workflow: WorkflowDefinition) { viewModelScope.launch { store.upsert(workflow) } }
    fun delete(id: String) { viewModelScope.launch { store.delete(id) } }
    fun run(workflow: WorkflowDefinition) { viewModelScope.launch {
        _lastRun.value = WorkflowEngine().run(workflow, actionRunner)
    } }
}

@Composable
fun WorkflowLabScreen(onBack: () -> Unit, vm: WorkflowLabViewModel = hiltViewModel()) {
    val saved by vm.workflows.collectAsState()
    val lastRun by vm.lastRun.collectAsState()
    var name by remember { mutableStateOf("New workflow") }
    var action by remember { mutableStateOf("inspect_workspace") }
    val steps = remember { mutableStateListOf(WorkflowStep(UUID.randomUUID().toString(), "Step 1", action)) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }; Text("Workflow Lab", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp)) }
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Workflow name") })
        OutlinedTextField(action, { action = it }, Modifier.fillMaxWidth(), label = { Text("Action command") })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { steps += WorkflowStep(UUID.randomUUID().toString(), "Step ${steps.size + 1}", action) }) { Icon(Icons.Filled.Add, null); Text("Add") }
            Button(onClick = { vm.save(WorkflowDefinition(UUID.randomUUID().toString(), name.ifBlank { "Workflow" }, steps.toList())) }) { Text("Save") }
        }
        Text("Current steps: ${steps.size}", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(steps, key = { it.id }) { step ->
                Row(Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text(step.name); Text(step.action, style = MaterialTheme.typography.bodySmall) }; IconButton(onClick = { steps.remove(step) }) { Icon(Icons.Filled.Delete, "Delete") } }
            }
            item { Text("Saved workflows", style = MaterialTheme.typography.titleMedium) }
            items(saved, key = { it.id }) { workflow -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(10.dp)) { Column(Modifier.weight(1f)) { Text(workflow.name); Text("${workflow.steps.size} steps") }; IconButton(onClick = { vm.run(workflow) }) { Icon(Icons.Filled.PlayArrow, "Run") }; IconButton(onClick = { vm.delete(workflow.id) }) { Icon(Icons.Filled.Delete, "Delete") } } } }
        }
        lastRun?.let { run -> Text("Last run: ${if (run.success) "SUCCESS" else "FAILED"} • ${run.steps.count { it.success }}/${run.steps.size} steps", color = if (run.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
    }
}
