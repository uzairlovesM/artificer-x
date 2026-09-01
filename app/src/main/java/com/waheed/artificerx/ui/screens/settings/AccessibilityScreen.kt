package com.waheed.artificerx.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.glassSurface

@Composable
fun AccessibilityScreen(
    onBack: () -> Unit,
    viewModel: AccessibilityViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier.fillMaxSize().background(ArtificerXGradients.backgroundWash),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    "Accessibility",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                ToggleRow(
                    "High Contrast Mode",
                    "Increases text and icon contrast against dark backgrounds",
                    settings.highContrastMode,
                    viewModel::setHighContrast,
                )
                Spacer(modifier = Modifier.padding(top = 12.dp))
                ToggleRow("Reduce Motion", "Minimizes animations and transitions", settings.reduceMotion, viewModel::setReduceMotion)
                Spacer(modifier = Modifier.padding(top = 12.dp))
                ToggleRow(
                    "Haptic Feedback",
                    "Vibration on tool selection and tool-call events",
                    settings.hapticFeedbackEnabled,
                    viewModel::setHapticFeedback,
                )
                Spacer(modifier = Modifier.padding(top = 16.dp))

                Column(modifier = Modifier.fillMaxWidth().glassSurface().padding(16.dp)) {
                    Text(
                        "Text Size",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.padding(top = 8.dp))
                    Slider(
                        value = settings.textScale,
                        onValueChange = { viewModel.setTextScale(it) },
                        valueRange = 0.8f..1.6f,
                        colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                    )
                    Text(
                        "${(settings.textScale * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().glassSurface().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onToggle, colors = SwitchDefaults.colors(checkedTrackColor = GoldPrimary))
    }
}
