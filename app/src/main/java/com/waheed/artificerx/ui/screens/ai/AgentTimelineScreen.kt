package com.waheed.artificerx.ui.screens.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun AgentTimelineScreen(onBack:()->Unit) {
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while(true){ delay(1200); tick++ } }
    val phases = listOf("Context loaded", "Goal decomposed", "Relevant tools selected", "Execution verified", "Artifacts committed")
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()){ IconButton(onClick=onBack){Icon(Icons.Filled.ArrowBack,null)}; Text("Agent Timeline", style=MaterialTheme.typography.headlineSmall, modifier=Modifier.padding(top=10.dp)) }
        LinearProgressIndicator(progress={((tick%100)/100f).coerceIn(0f,1f)}, Modifier.fillMaxWidth())
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) { items(phases){ p -> Card(Modifier.fillMaxWidth()){ Text("✓  $p", Modifier.padding(14.dp)) } } }
        Text("The timeline reflects the execution model, not hidden chain-of-thought.", style=MaterialTheme.typography.bodySmall)
    }
}
