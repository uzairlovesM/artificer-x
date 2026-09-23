package com.waheed.artificerx.ui.screens.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.data.local.datastore.QualityPreset
import com.waheed.artificerx.ui.components.SoftCard
import com.waheed.artificerx.ui.components.WorkspaceTopBar
import com.waheed.artificerx.ui.screens.settings.QualityBudgetViewModel
import java.util.Locale

@Composable
fun AgentWorkbenchScreen(
    onBack: () -> Unit,
    onChat: () -> Unit,
    onTools: () -> Unit,
    viewModel: QualityBudgetViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val preset = settings.qualityPreset
    val confidence = when (preset) {
        QualityPreset.FAST -> 0.68f
        QualityPreset.BALANCED -> 0.82f
        QualityPreset.THOROUGH -> 0.92f
        QualityPreset.DEEP_STUDIO -> 0.98f
    }

    Scaffold(topBar = { WorkspaceTopBar("Agent Workbench", "Live agent execution policy and persisted runtime controls", onBack) }) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SoftCard {
                    Text("Reasoning profile", style = MaterialTheme.typography.titleLarge)
                    Text(
                        when (preset) {
                            QualityPreset.FAST -> "Fast execution with a short bounded tool loop"
                            QualityPreset.BALANCED -> "Balanced planning, execution and validation"
                            QualityPreset.THOROUGH -> "Thorough self-correction with frequent canvas inspection"
                            QualityPreset.DEEP_STUDIO -> "Deep Studio: research → plan → multi-layer build → inspect → repair"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    LinearProgressIndicator(progress = { confidence }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }

            item {
                SoftCard {
                    Text("Quality preset", style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(QualityPreset.entries.size) { index ->
                            val candidate = QualityPreset.entries[index]
                            FilterChip(
                                selected = candidate == preset,
                                onClick = { viewModel.selectPreset(candidate) },
                                label = { Text(candidate.label) },
                            )
                        }
                    }
                    Text(
                        "${settings.effectiveMaxIterations} iterations · temperature ${String.format(Locale.ROOT, "%.2f", settings.effectiveTemperature)} · snapshot every ${settings.effectiveSnapshotFrequency} tool call(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SoftCard {
                    AgentSettingRow(
                        title = "Extended thinking",
                        subtitle = "Persisted provider reasoning preference. Deep Studio already enables high reasoning effort.",
                        checked = settings.thinkingEnabled,
                        onCheckedChange = viewModel::setThinkingEnabled,
                    )
                    Text(
                        "Reasoning effort: ${settings.reasoningEffort ?: "provider default"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                SoftCard {
                    Text("Effective execution", style = MaterialTheme.typography.titleMedium)
                    ExecutionFact("Planning", if (preset == QualityPreset.FAST) "Condensed" else "Enabled")
                    ExecutionFact("Self-correction", if (settings.enableCriticPass) "Critic pass enabled" else "Bounded retry loop")
                    ExecutionFact("Vision feedback", "Provider-capability gated")
                    ExecutionFact("Persistent memory", "Workspace-backed")
                    ExecutionFact("Artifacts", "Validated before response")
                    ExecutionFact("Web tools", "Registry + provider policy")
                }
            }

            item {
                SoftCard {
                    Text("Runtime actions", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(onClick = onChat, modifier = Modifier.weight(1f)) { Text("Open chat") }
                        androidx.compose.material3.Button(onClick = onTools, modifier = Modifier.weight(1f)) { Text("Inspect tools") }
                    }
                }
            }

            item {
                Text(
                    "These controls are persisted in the agent settings store and are consumed by AgentOrchestrator on the next turn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AgentSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ExecutionFact(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
