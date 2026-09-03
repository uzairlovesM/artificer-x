package com.waheed.artificerx.ui.screens.system

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExtremeControlCenterScreen(
    onBack:()->Unit, onAutomation:()->Unit, onSearch:()->Unit, onTimeline:()->Unit, onPermissions:()->Unit, onFiles:()->Unit, onObservatory:()->Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()){ IconButton(onClick=onBack){Icon(Icons.Filled.ArrowBack,null)}; Text("Artificer X Control Center", style=MaterialTheme.typography.headlineSmall, modifier=Modifier.padding(top=10.dp)) }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)) {
            item { Text("One control surface for intelligence, storage, automation and safety.", style=MaterialTheme.typography.bodyMedium) }
            item { ControlCard("Agent execution timeline", "Inspect real execution phases and verification", Icons.Filled.AutoAwesome, onTimeline) }
            item { ControlCard("Automation", "Run and configure local WorkManager automations", Icons.Filled.Schedule, onAutomation) }
            item { ControlCard("Workspace search", "Search managed works, artifacts, models and plugins", Icons.Filled.Search, onSearch) }
            item { ControlCard("Storage & paths", "Inspect ARTIFICER-X/works/cache/system/plugins/models…", Icons.Filled.Folder, onFiles) }
            item { ControlCard("Permissions", "Runtime permissions, SAF storage and app controls", Icons.Filled.Security, onPermissions) }
            item { ControlCard("System Observatory", "Runtime memory, paths, tools and plugin telemetry", Icons.Filled.MonitorHeart, onObservatory) }
        }
    }
}
@Composable private fun ControlCard(title:String, subtitle:String, icon:androidx.compose.ui.graphics.vector.ImageVector, onClick:()->Unit){ Card(onClick=onClick, modifier=Modifier.fillMaxWidth()){ Row(Modifier.padding(16.dp)){ Icon(icon,null); Spacer(Modifier.width(14.dp)); Column{ Text(title, style=MaterialTheme.typography.titleMedium); Text(subtitle, style=MaterialTheme.typography.bodySmall) } } } }
