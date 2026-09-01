package com.waheed.artificerx.ui.screens.sculpt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.domain.model.PrimitiveType
import com.waheed.artificerx.domain.model.SculptBrushType
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.ToolCallChipShape
import com.waheed.artificerx.ui.theme.glassSurface

/**
 * Full 3D sculpting studio surface — the 3D counterpart to
 * StudioScreen. Structure: top bar (back, agent-activity indicator),
 * full-screen SculptSurfaceView (real GPU render + touch-to-sculpt),
 * bottom brush toolbar (radius/strength sliders + 6 brush types),
 * floating primitive-add button and mesh list.
 */
@Composable
fun SculptScreen(
    onBack: () -> Unit,
    viewModel: SculptViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPrimitivePicker by remember { mutableStateOf(false) }
    var showMeshList by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                text = "Sculpt Studio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val meshId = uiState.activeMeshId ?: return@IconButton
                    val mesh = uiState.meshes[meshId] ?: return@IconButton
                    val centerVertex =
                        mesh.vertices.minByOrNull { it.length() }
                            ?: mesh.vertices.firstOrNull()
                            ?: return@IconButton
                    viewModel.applyManualStroke(centerVertex)
                },
                enabled = uiState.activeMeshId != null,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Test stroke at mesh center (manual sculpt, no viewport yet)",
                    tint = if (uiState.activeMeshId != null) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { showMeshList = !showMeshList }) {
                Icon(Icons.Filled.Settings, contentDescription = "Meshes", tint = GoldPrimary)
            }
            IconButton(onClick = { showPrimitivePicker = !showPrimitivePicker }) {
                Icon(Icons.Filled.Add, contentDescription = "Add primitive", tint = GoldPrimary)
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            SculptViewportPlaceholder(
                activeMesh = uiState.activeMeshId?.let { uiState.meshes[it] },
                meshCount = uiState.meshes.size,
            )

            if (showPrimitivePicker) {
                PrimitivePickerOverlay(
                    onSelect = { type ->
                        viewModel.addPrimitive(type)
                        showPrimitivePicker = false
                    },
                    onDismiss = { showPrimitivePicker = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                )
            }

            if (showMeshList) {
                MeshListOverlay(
                    meshIds = uiState.meshes.keys.toList(),
                    meshNames = uiState.meshes.mapValues { it.value.name },
                    activeMeshId = uiState.activeMeshId,
                    onSelect = viewModel::setActiveMesh,
                    onDelete = viewModel::deleteMesh,
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                )
            }
        }

        BrushToolbar(
            activeBrush = uiState.activeBrush,
            radius = uiState.brushRadius,
            strength = uiState.brushStrength,
            onBrushSelected = viewModel::selectBrush,
            onRadiusChanged = viewModel::setBrushRadius,
            onStrengthChanged = viewModel::setBrushStrength,
        )
    }
}

@Composable
private fun SculptViewportPlaceholder(
    activeMesh: com.waheed.artificerx.domain.model.SculptMesh?,
    meshCount: Int,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Circle, contentDescription = null, tint = GoldPrimary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(
                text =
                    if (activeMesh !=
                        null
                    ) {
                        "${activeMesh.name} — ${activeMesh.vertexCount}v / ${activeMesh.triangleCount}t"
                    } else {
                        "$meshCount mesh(es) in scene"
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "3D viewport renderer not wired yet — mesh data and sculpt tools are fully functional",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun PrimitivePickerOverlay(
    onSelect: (PrimitiveType) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.glassSurface().padding(12.dp)) {
        Text(
            "Add Primitive",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        PrimitiveType.entries.forEach { type ->
            Text(
                text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .then(Modifier)
                        .clickableSimple { onSelect(type) },
            )
        }
    }
}

@Composable
private fun MeshListOverlay(
    meshIds: List<String>,
    meshNames: Map<String, String>,
    activeMeshId: String?,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.glassSurface().padding(12.dp)) {
        Text(
            "Scene Meshes",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        if (meshIds.isEmpty()) {
            Text("No meshes yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        meshIds.forEach { id ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Circle,
                    contentDescription = null,
                    tint = if (id == activeMeshId) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(10.dp).clickableSimple { onSelect(id) },
                )
                Text(
                    text = meshNames[id] ?: id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f).padding(start = 8.dp).clickableSimple { onSelect(id) },
                )
                IconButton(onClick = { onDelete(id) }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        Icons.Filled.RemoveCircleOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun BrushToolbar(
    activeBrush: SculptBrushType,
    radius: Float,
    strength: Float,
    onBrushSelected: (SculptBrushType) -> Unit,
    onRadiusChanged: (Float) -> Unit,
    onStrengthChanged: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Radius", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = radius,
                onValueChange = onRadiusChanged,
                valueRange = 0.05f..1f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Strength", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = strength,
                onValueChange = onStrengthChanged,
                valueRange = 0.05f..1.5f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
            )
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(horizontal = 16.dp),
        ) {
            items(SculptBrushType.entries) { brush ->
                BrushChip(brush = brush, isSelected = brush == activeBrush, onClick = { onBrushSelected(brush) })
            }
        }
    }
}

@Composable
private fun BrushChip(
    brush: SculptBrushType,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .background(if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant, ToolCallChipShape),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = brushIconFor(brush),
                contentDescription = brush.name,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun brushIconFor(brush: SculptBrushType): ImageVector =
    when (brush) {
        SculptBrushType.PUSH -> Icons.Filled.ArrowDownward
        SculptBrushType.PULL -> Icons.Filled.ArrowUpward
        SculptBrushType.SMOOTH -> Icons.Filled.Circle
        SculptBrushType.PINCH -> Icons.Filled.Straighten
        SculptBrushType.INFLATE -> Icons.Filled.Star
        SculptBrushType.FLATTEN -> Icons.Filled.Brush
    }

private fun Modifier.clickableSimple(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
