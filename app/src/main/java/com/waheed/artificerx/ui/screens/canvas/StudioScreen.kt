package com.waheed.artificerx.ui.screens.canvas

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waheed.artificerx.domain.model.AgentActivityState
import com.waheed.artificerx.domain.model.DrawToolType
import com.waheed.artificerx.ui.theme.AgentIdle
import com.waheed.artificerx.ui.theme.AgentThinking
import com.waheed.artificerx.ui.theme.AgentToolCalling
import com.waheed.artificerx.ui.theme.ArtificerXGradients
import com.waheed.artificerx.ui.theme.GoldPrimary
import com.waheed.artificerx.ui.theme.LayerRowShape
import com.waheed.artificerx.ui.theme.PurpleAccent
import com.waheed.artificerx.ui.theme.QualityFail
import com.waheed.artificerx.ui.theme.QualityWarn
import com.waheed.artificerx.ui.theme.ToolCallChipShape
import com.waheed.artificerx.ui.theme.glassSurface

/**
 * Main working surface (Section 111 Mobile UI). Structure:
 *  - Top bar: project name, agent activity pulse, layer-panel toggle,
 *    settings shortcut.
 *  - Canvas area: bitmap render surface — a placeholder gradient
 *    checker board for now (the real Canvas/BitmapCompositor engine
 *    is a dedicated future phase; this establishes the exact frame
 *    the render surface will occupy so nothing shifts later).
 *  - Bottom tool palette: horizontally scrollable tool chips + a
 *    context-sensitive brush-size slider that only shows for brush-
 *    like tools.
 *  - Floating action button to open Agent Chat (Section 90/91's
 *    conversational interface entry point).
 */
@Composable
fun StudioScreen(
    snackbarHostState: SnackbarHostState,
    onOpenAgentChat: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProjectGallery: () -> Unit,
    onOpenExport: (String) -> Unit = {},
    onOpenSculptStudio: () -> Unit = {},
    viewModel: StudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isLayerPanelOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        StudioTopBar(
            projectName = state.projectName,
            agentActivity = state.agentActivity,
            onToggleLayerPanel = { isLayerPanelOpen = !isLayerPanelOpen },
            onOpenSettings = onOpenSettings,
            onOpenProjectGallery = onOpenProjectGallery,
            onOpenExport = { onOpenExport(state.projectId) },
            onOpenSculptStudio = onOpenSculptStudio,
        )

        Box(modifier = Modifier.weight(1f)) {
            val compositedBitmap by viewModel.compositedBitmap.collectAsStateWithLifecycle()
            CanvasRenderSurface(
                bitmap = compositedBitmap,
                widthPx = state.canvasWidthPx,
                heightPx = state.canvasHeightPx,
            )

            if (isLayerPanelOpen) {
                LayerPanelOverlay(
                    layers = state.layers,
                    activeLayerId = state.activeLayerId,
                    onSelectLayer = viewModel::setActiveLayer,
                    onToggleVisibility = viewModel::toggleLayerVisibility,
                    onToggleLock = viewModel::toggleLayerLock,
                    onOpacityChange = viewModel::setLayerOpacity,
                    onAddLayer = viewModel::addLayer,
                    onDeleteLayer = viewModel::deleteLayer,
                    onDismiss = { isLayerPanelOpen = false },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }

            AgentChatFab(
                onClick = onOpenAgentChat,
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
            )
        }

        ToolPalette(
            activeTool = state.toolState.activeTool,
            brushSize = state.toolState.brushSizePx,
            onToolSelected = viewModel::selectTool,
            onBrushSizeChanged = viewModel::setBrushSize,
        )
    }
}

@Composable
private fun StudioTopBar(
    projectName: String,
    agentActivity: AgentActivityState,
    onToggleLayerPanel: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProjectGallery: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenSculptStudio: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpenProjectGallery) {
            Icon(Icons.Filled.Folder, contentDescription = "Projects", tint = MaterialTheme.colorScheme.onBackground)
        }

        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
            Text(
                text = projectName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            AgentActivityIndicator(agentActivity)
        }

        IconButton(onClick = onOpenExport) {
            Icon(Icons.Filled.Download, contentDescription = "Export", tint = GoldPrimary)
        }
        IconButton(onClick = onOpenSculptStudio) {
            Icon(Icons.Filled.ViewInAr, contentDescription = "3D Sculpt Studio", tint = GoldPrimary)
        }
        IconButton(onClick = onToggleLayerPanel) {
            Icon(Icons.Filled.Layers, contentDescription = "Layers", tint = GoldPrimary)
        }
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
        }
    }
}

@Composable
private fun AgentActivityIndicator(activity: AgentActivityState) {
    val (color, label) =
        when (activity) {
            AgentActivityState.IDLE -> AgentIdle to "Idle"
            AgentActivityState.THINKING -> AgentThinking to "Thinking…"
            AgentActivityState.CALLING_TOOL -> AgentToolCalling to "Using tool…"
            AgentActivityState.RENDERING -> AgentToolCalling to "Rendering…"
            AgentActivityState.SELF_CORRECTING -> QualityWarn to "Self-correcting…"
            AgentActivityState.AWAITING_USER_INPUT -> PurpleAccent to "Waiting on you"
            AgentActivityState.ERROR -> QualityFail to "Error"
        }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Circle, contentDescription = null, tint = color, modifier = Modifier.size(8.dp))
        Spacer(modifier = Modifier.padding(start = 4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CanvasRenderSurface(
    bitmap: android.graphics.Bitmap?,
    widthPx: Int,
    heightPx: Int,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clip(MaterialTheme.shapes.large)
                .background(ArtificerXGradients.backgroundWash),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Canvas artwork, $widthPx by $heightPx pixels",
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = GoldPrimary.copy(alpha = 0.4f),
                    modifier = Modifier.size(48.dp),
                )
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = "$widthPx×$heightPx canvas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Waiting for first render…",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun ToolPalette(
    activeTool: DrawToolType,
    brushSize: Float,
    onToolSelected: (DrawToolType) -> Unit,
    onBrushSizeChanged: (Float) -> Unit,
) {
    val tools =
        remember {
            listOf(
                DrawToolType.BRUSH to Icons.Filled.Brush,
                DrawToolType.ERASER to Icons.Filled.Delete,
                DrawToolType.SHAPE_RECT to Icons.Filled.Rectangle,
                DrawToolType.GRADIENT to Icons.Filled.Gradient,
                DrawToolType.FILL to Icons.Filled.FormatColorFill,
                DrawToolType.SELECTION to Icons.Filled.Crop,
                DrawToolType.TEXT to Icons.Filled.TextFields,
                DrawToolType.EYEDROPPER to Icons.Filled.ColorLens,
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        if (activeTool == DrawToolType.BRUSH || activeTool == DrawToolType.ERASER) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Size", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = brushSize,
                    onValueChange = onBrushSizeChanged,
                    valueRange = 1f..80f,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                )
                Text(
                    "${brushSize.toInt()}px",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(horizontal = 16.dp),
        ) {
            items(tools) { (tool, icon) ->
                ToolChip(icon = icon, isSelected = tool == activeTool, onClick = { onToolSelected(tool) })
            }
        }
    }
}

@Composable
private fun ToolChip(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(ToolCallChipShape)
                .background(if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant)
                .then(Modifier),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LayerPanelOverlay(
    layers: List<com.waheed.artificerx.domain.model.CanvasLayer>,
    activeLayerId: String?,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onOpacityChange: (String, Float) -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(12.dp)
                .glassSurface()
                .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Layers",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onAddLayer) {
                Icon(Icons.Filled.Layers, contentDescription = "Add layer", tint = GoldPrimary)
            }
        }
        layers.sortedByDescending { it.orderIndex }.forEach { layer ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(LayerRowShape)
                        .background(
                            if (layer.id == activeLayerId) PurpleAccent.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
                        ).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = layer.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onToggleVisibility(layer.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = "Toggle visibility",
                        tint = if (layer.isVisible) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
                IconButton(onClick = { onDeleteLayer(layer.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete layer", tint = QualityFail, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AgentChatFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(ArtificerXGradients.heroGoldPurple),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.Chat, contentDescription = "Agent Chat", tint = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
