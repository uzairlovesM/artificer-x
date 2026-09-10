package com.waheed.artificerx.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.domain.model.BrushType
import com.waheed.artificerx.domain.model.SymmetryMode
import com.waheed.artificerx.ui.theme.GoldPrimary

@Composable
fun AdvancedBrushDock(
    activeBrush: BrushType,
    sizePx: Float,
    opacity: Float,
    hardness: Float,
    flow: Float,
    spacing: Float,
    smoothing: Float,
    scatter: Float,
    sizePressure: Float,
    opacityPressure: Float,
    pressureSimulation: Boolean,
    symmetry: SymmetryMode,
    colorHex: String,
    onBrushSelected: (BrushType) -> Unit,
    onSizeChanged: (Float) -> Unit,
    onOpacityChanged: (Float) -> Unit,
    onHardnessChanged: (Float) -> Unit,
    onFlowChanged: (Float) -> Unit,
    onSpacingChanged: (Float) -> Unit,
    onSmoothingChanged: (Float) -> Unit,
    onScatterChanged: (Float) -> Unit,
    onPressureResponseChanged: (Float, Float) -> Unit,
    onPressureSimulationChanged: (Boolean) -> Unit,
    onSymmetryChanged: (SymmetryMode) -> Unit,
    onColorChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brushes = listOf(
        BrushType.INK_PEN to "Ink",
        BrushType.PENCIL to "Pencil",
        BrushType.MARKER to "Marker",
        BrushType.CALLIGRAPHY to "Calligraphy",
        BrushType.AIRBRUSH to "Airbrush",
        BrushType.WATERCOLOR to "Watercolor",
        BrushType.CHARCOAL to "Charcoal",
    )
    val symmetryModes = listOf(
        SymmetryMode.OFF to "Off",
        SymmetryMode.VERTICAL to "V",
        SymmetryMode.HORIZONTAL to "H",
        SymmetryMode.RADIAL_4 to "R4",
        SymmetryMode.RADIAL_8 to "R8",
        SymmetryMode.RADIAL_12 to "R12",
        SymmetryMode.KALEIDOSCOPE_6 to "K6",
        SymmetryMode.KALEIDOSCOPE_12 to "K12",
        SymmetryMode.MANDALA_24 to "M24",
    )
    val parsedColor = runCatching { Color(android.graphics.Color.parseColor(colorHex)) }.getOrDefault(Color.White)
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
    val colorPresets = listOf("#FFFFFFFF", "#FFD166", "#EF476F", "#06D6A0", "#118AB2", "#8338EC", "#FF9F1C", "#111111")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(36.dp)) {
                drawCircle(parsedColor, radius = size.minDimension / 2f - 2f)
                drawCircle(outlineColor, radius = size.minDimension / 2f - 2f, style = Stroke(2f))
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                Text("Brush dynamics", style = MaterialTheme.typography.labelLarge)
                Text("$activeBrush  •  ${sizePx.toInt()} px  •  ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onPressureSimulationChanged(!pressureSimulation) }) {
                Text(if (pressureSimulation) "P" else "p", color = if (pressureSimulation) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            brushes.forEach { (brush, label) ->
                FilterChip(
                    selected = brush == activeBrush,
                    onClick = { onBrushSelected(brush) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldPrimary.copy(alpha = 0.22f),
                        selectedLabelColor = GoldPrimary,
                    ),
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        AdvancedSlider("Size", sizePx, 1f..180f, { onSizeChanged(it) }, "${sizePx.toInt()}px")
        AdvancedSlider("Opacity", opacity, 0f..1f, { onOpacityChanged(it) }, "${(opacity * 100).toInt()}%")
        AdvancedSlider("Hardness", hardness, 0f..1f, { onHardnessChanged(it) }, "${(hardness * 100).toInt()}%")
        AdvancedSlider("Flow", flow, 0f..1f, { onFlowChanged(it) }, "${(flow * 100).toInt()}%")
        AdvancedSlider("Spacing", spacing, 0.01f..1f, { onSpacingChanged(it) }, "${(spacing * 100).toInt()}%")
        AdvancedSlider("Smoothing", smoothing, 0f..1f, { onSmoothingChanged(it) }, "${(smoothing * 100).toInt()}%")
        AdvancedSlider("Scatter", scatter, 0f..1f, { onScatterChanged(it) }, "${(scatter * 100).toInt()}%")
        AdvancedSlider("Size pressure", sizePressure, 0f..1f, { value -> onPressureResponseChanged(value, opacityPressure) }, "${(sizePressure * 100).toInt()}%")
        AdvancedSlider("Opacity pressure", opacityPressure, 0f..1f, { value -> onPressureResponseChanged(sizePressure, value) }, "${(opacityPressure * 100).toInt()}%")

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            symmetryModes.forEach { (mode, label) ->
                FilterChip(
                    selected = mode == symmetry,
                    onClick = { onSymmetryChanged(mode) },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldPrimary.copy(alpha = 0.22f), selectedLabelColor = GoldPrimary),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            colorPresets.forEach { preset ->
                val c = Color(android.graphics.Color.parseColor(preset))
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(c),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(onClick = { onColorChanged(preset) }, modifier = Modifier.size(28.dp)) {}
                }
            }
        }
    }
}

@Composable
private fun AdvancedSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChanged: (Float) -> Unit,
    valueLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.size(105.dp, 28.dp), style = MaterialTheme.typography.labelSmall)
        Slider(
            value = value,
            onValueChange = onValueChanged,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
        )
        Text(valueLabel, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
