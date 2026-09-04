package com.waheed.artificerx.ui.screens.hub

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.agent.ToolRegistry

@Composable
fun ToolUniverseScreen(onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val tools = remember(query) {
        ToolRegistry.ALL_TOOLS.filter { tool ->
            // Terminal execution is an AI-only capability by design for this private build.
            tool.function.name !in setOf("run_terminal_command", "run_terminal_batch") &&
                (query.isBlank() || tool.function.name.contains(query, true) || tool.function.description.contains(query, true))
        }
    }
    Scaffold(topBar = {
        SmallTopAppBar(title = { Text("Tool Universe") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Search, null)
                OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search tools") }, modifier = Modifier.weight(1f), singleLine = true)
            }
            Text("${tools.size} visible tools • ${ToolRegistry.ALL_TOOLS.size} agent capabilities • terminal is AI-only", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 10.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tools, key = { it.function.name }) { tool ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(tool.function.name, style = MaterialTheme.typography.titleSmall)
                        Text(tool.function.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
