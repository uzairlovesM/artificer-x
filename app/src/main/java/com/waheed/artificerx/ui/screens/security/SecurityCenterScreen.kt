package com.waheed.artificerx.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.core.security.SecurityPolicy

@Composable
fun SecurityCenterScreen(onBack: () -> Unit) {
    var command by remember { mutableStateOf("echo Artificer-X sandbox") }
    val allowed = remember(command) { SecurityPolicy.isShellAllowed(command) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }; Text("Security Center", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp)) }
        Text("Command safety and path-boundary policy")
        OutlinedTextField(command, { command = it }, Modifier.fillMaxWidth(), label = { Text("Probe shell command") })
        Text(if (allowed) "ALLOWED by local policy" else "BLOCKED by local policy", color = if (allowed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        Text("Sandbox rules block destructive root operations, format commands and known fork-bomb patterns. File actions remain constrained to app-owned roots.", style = MaterialTheme.typography.bodySmall)
    }
}
