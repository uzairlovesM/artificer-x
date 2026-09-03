package com.waheed.artificerx.ui.screens.art

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.waheed.artificerx.domain.model.DrawToolType
import com.waheed.artificerx.ui.components.SoftCard
import com.waheed.artificerx.ui.components.WorkspaceChip
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProArtStudioScreen(
    vm: StudioViewModel,
    onBack: () -> Unit,
    onBrushes: () -> Unit,
    onLayers: () -> Unit,
    onFilters: () -> Unit,
    onRulers: () -> Unit,
    onAnimation: () -> Unit,
    onMaterials: () -> Unit,
    onManga: () -> Unit,
    onColor: () -> Unit = {},
    onText: () -> Unit = {},
    onReference: () -> Unit = {},
) {
    val state by vm.state.collectAsCompat()
    val bitmap by vm.compositedBitmap.collectAsCompat()
    var panel by remember { mutableStateOf("Canvas") }
    var zoom by remember { mutableFloatStateOf(1f) }
    var showGrid by remember { mutableStateOf(false) }
    val strokePreview = remember { mutableStateListOf<Offset>() }
    val inspectorOffset by animateFloatAsState(if (panel == "Canvas") 0f else 1f, tween(280, easing = FastOutSlowInEasing), label = "inspector_offset")

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Column {
                        Text(state.projectName, style = MaterialTheme.typography.titleLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("Pro Art", style = MaterialTheme.typography.labelSmall)
                            StatusDot(state.agentActivity.name.lowercase().replace('_', ' '))
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.Close, "Close") } },
                actions = {
                    IconButton(onClick = vm::undo, enabled = state.undoStackSize > 0) { Icon(Icons.Filled.Undo, "Undo") }
                    IconButton(onClick = vm::redo, enabled = state.redoStackSize > 0) { Icon(Icons.Filled.Redo, "Redo") }
                    IconButton(onClick = vm::saveNow) { Icon(Icons.Filled.Save, "Save") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { pad ->
        Row(Modifier.fillMaxSize().padding(pad)) {
            Column(Modifier.width(84.dp).fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                RailButton("Canvas", Icons.Filled.Settings, panel == "Canvas") { panel = "Canvas" }
                RailButton("Brush", Icons.Filled.Brush, panel == "Brush", onBrushes)
                RailButton("Layers", Icons.Filled.Layers, panel == "Layers", onLayers)
                RailButton("Color", Icons.Filled.Palette, panel == "Color", onColor)
                RailButton("Tune", Icons.Filled.Tune, panel == "Tune", onFilters)
                Spacer(Modifier.weight(1f))
                RailButton("AI", Icons.Filled.AutoFixHigh, false, onReference)
            }

            Column(Modifier.weight(1f).fillMaxHeight()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ToolAction("Brush", Icons.Filled.Brush) { vm.selectTool(DrawToolType.BRUSH) }
                    ToolAction("Erase", Icons.Filled.ContentCut) { vm.selectTool(DrawToolType.ERASER) }
                    ToolAction("Fill", Icons.Filled.Palette) { vm.selectTool(DrawToolType.FILL) }
                    ToolAction("Grid", Icons.Filled.GridOn, active = showGrid) { showGrid = !showGrid }
                    ToolAction("Filters", Icons.Filled.AutoFixHigh) { onFilters() }
                    ToolAction("Ruler", Icons.Filled.Tune) { onRulers() }
                    ToolAction("Layers", Icons.Filled.Layers) { onLayers() }
                    ToolAction("Anim", Icons.Filled.PlayArrow) { onAnimation() }
                    ToolAction("Color", Icons.Filled.Palette) { onColor() }
                    ToolAction("Text", Icons.Filled.Save) { onText() }
                    ToolAction("Ref", Icons.Filled.Settings) { onReference() }
                }

                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                    val canvasScale = animateFloatAsState(zoom, tween(220), label = "canvas_zoom")
                    Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 4.dp, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Box(Modifier.width(430.dp).height(540.dp).clip(RoundedCornerShape(22.dp))) {
                            Canvas(
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(state.activeLayerId, state.toolState.activeTool, canvasScale.value) {
                                        detectDragGestures(
                                            onDragStart = { start -> strokePreview.clear(); strokePreview += Offset(start.x / canvasScale.value, start.y / canvasScale.value) },
                                            onDrag = { change, _ -> change.consume(); strokePreview += Offset(change.position.x / canvasScale.value, change.position.y / canvasScale.value) },
                                            onDragEnd = {
                                                if (strokePreview.size >= 2 && state.toolState.activeTool == DrawToolType.BRUSH) {
                                                    vm.drawManualStroke(strokePreview.flatMap { listOf(it.x, it.y) })
                                                } else if (strokePreview.size >= 2 && state.toolState.activeTool == DrawToolType.ERASER) {
                                                    vm.drawManualStroke(strokePreview.flatMap { listOf(it.x, it.y) })
                                                }
                                                strokePreview.clear()
                                            },
                                            onDragCancel = { strokePreview.clear() },
                                        )
                                    },
                            ) {
                                drawRect(Color(0xFFECE7DF))
                                if (showGrid) drawGrid()
                                bitmap?.let { b -> drawImage(b.asImageBitmap(), dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())) }
                                if (strokePreview.size > 1) {
                                    val path = Path().apply { moveTo(strokePreview.first().x, strokePreview.first().y); strokePreview.drop(1).forEach { lineTo(it.x, it.y) } }
                                    drawPath(path, Color.Black.copy(alpha = 0.24f), style = Stroke(width = (state.toolState.brushSizePx * canvasScale.value).coerceAtLeast(1f)))
                                }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { zoom = (zoom - .1f).coerceAtLeast(.5f) }) { Icon(Icons.Filled.Remove, "Zoom out") }
                    Slider(zoom, { zoom = it }, valueRange = .5f..2.5f, Modifier.weight(1f))
                    Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.widthIn(min = 45.dp))
                    IconButton(onClick = { zoom = (zoom + .1f).coerceAtMost(2.5f) }) { Icon(Icons.Filled.Add, "Zoom in") }
                }

                AnimatedVisibility(panel != "Canvas", enter = fadeIn(tween(180)) + slideInHorizontally(initialOffsetX = { it / 3 }), exit = fadeOut(tween(140))) {
                    SoftCard(Modifier.fillMaxWidth().padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("$panel inspector", style = MaterialTheme.typography.titleMedium)
                                Text("Changes stay attached to this live project session.", style = MaterialTheme.typography.bodySmall)
                            }
                            FilledTonalButton(onClick = { panel = "Canvas" }) { Text("Done") }
                        }
                        Spacer(Modifier.height(8.dp))
                        AnimatedContent(targetState = panel, label = "inspector_content") { selected ->
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                when (selected) {
                                    "Brush" -> { WorkspaceChip("Brush Lab", onClick = onBrushes); WorkspaceChip("Ruler", onClick = onRulers) }
                                    "Layers" -> { WorkspaceChip("Layer Lab", onClick = onLayers); WorkspaceChip("Manga", onClick = onManga) }
                                    "Color" -> { WorkspaceChip("Color Studio", onClick = onColor); WorkspaceChip("Materials", onClick = onMaterials) }
                                    else -> { WorkspaceChip("Filters", onClick = onFilters); WorkspaceChip("Animation", onClick = onAnimation); WorkspaceChip("Reference", onClick = onReference) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun RailButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val background by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = .14f) else Color.Transparent, tween(180), label = "rail_bg")
    Surface(onClick = onClick, color = background, shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable private fun StatusDot(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(Modifier.size(6.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary) {}
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun ToolAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean = false, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(13.dp), color = if (active) MaterialTheme.colorScheme.primary.copy(alpha=.16f) else MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(horizontal=10.dp, vertical=7.dp), verticalAlignment=Alignment.CenterVertically) { Icon(icon, null, Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)); Text(label, style=MaterialTheme.typography.labelSmall) }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGrid() {
    val step = 32.dp.toPx()
    var x = 0f
    while (x <= size.width) { drawLine(Color.Black.copy(alpha=.06f), Offset(x,0f), Offset(x,size.height), 1f); x += step }
    var y = 0f
    while (y <= size.height) { drawLine(Color.Black.copy(alpha=.06f), Offset(0f,y), Offset(size.width,y), 1f); y += step }
}

@Composable private fun <T> kotlinx.coroutines.flow.StateFlow<T>.collectAsCompat(): androidx.compose.runtime.State<T> = androidx.compose.runtime.collectAsStateWithLifecycle()
