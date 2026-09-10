package com.waheed.artificerx.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.domain.model.BrushType
import com.waheed.artificerx.domain.model.SymmetryMode
import com.waheed.artificerx.ui.theme.GoldPrimary

@Composable
fun CanvasQuickBar(
    widthPx: Int,
    heightPx: Int,
    layerCount: Int,
    brushType: BrushType,
    brushSizePx: Float,
    symmetryMode: SymmetryMode,
    guideVisible: Boolean,
    onGuideToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        FilterChip(
            selected = guideVisible,
            onClick = onGuideToggle,
            leadingIcon = { Icon(Icons.Filled.GridOn, contentDescription = null) },
            label = { Text("Guides") },
            colors = FilterChipDefaults.filterChipColors(selectedLabelColor = GoldPrimary, selectedLeadingIconColor = GoldPrimary),
        )
        FilterChip(
            selected = symmetryMode != SymmetryMode.OFF,
            onClick = {},
            leadingIcon = { Icon(Icons.Filled.Straighten, contentDescription = null) },
            label = { Text("${symmetryMode.name.replace('_', ' ')}") },
            colors = FilterChipDefaults.filterChipColors(selectedLabelColor = GoldPrimary, selectedLeadingIconColor = GoldPrimary),
        )
        FilterChip(
            selected = false,
            enabled = false,
            onClick = {},
            leadingIcon = { Icon(Icons.Filled.Layers, contentDescription = null) },
            label = { Text("$layerCount layers") },
        )
        FilterChip(
            selected = false,
            enabled = false,
            onClick = {},
            label = { Text("${widthPx}×${heightPx} • ${brushSizePx.toInt()} px ${brushType.name}") },
        )
    }
}
