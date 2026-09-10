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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Rectangle
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Undo
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
import com.waheed.artificerx.ui.components.AdvancedBrushDock
import com.waheed.artificerx.ui.components.CanvasQuickBar

/**
 * Main working surface (Section 111 Mobile UI). Structure:
 *  - Top bar: project name, agent activity pulse, layer-panel toggle,
 *    settings shortcut.
 *  - Canvas area: real bitmap render surface backed by
 *    CanvasCompositor/LayerBitmapStore, with finger-drawing input
 *    wired directly to StudioViewModel's manual-draw entry points
 *    (drawManualStroke/drawManualShape/fillManualTap/pickManualColor)
 *    via canvasTouchInput — touch coordinates are re-mapped from the
 *    Fit-scaled display box into canvas-bitmap pixel space before
 *    being forwarded, so strokes land under the finger regardless of
 *    the device's aspect ratio.
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
    onOpenTimelapse: () -> Unit = {},
    viewModel: StudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var isLayerPanelOpen by remember { mutableStateOf(false) }
    var showCanvasGuides by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        StudioTopBar(
            projectName = state.projectName,
            agentActivity = state.agentActivity,
            canUndo = state.undoStackSize > 0,
            canRedo = state.redoStackSize > 0,
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onToggleLayerPanel = { isLayerPanelOpen = !isLayerPanelOpen },
            onOpenSettings = onOpenSettings,
            onOpenProjectGallery = onOpenProjectGallery,
            onOpenExport = { onOpenExport(state.projectId) },
            onOpenSculptStudio = onOpenSculptStudio,
            onOpenTimelapse = onOpenTimelapse,
        )

        Box(modifier = Modifier.weight(1f)) {
            val compositedBitmap by viewModel.compositedBitmap.collectAsStateWithLifecycle()
            CanvasQuickBar(
                widthPx = state.canvasWidthPx,
                heightPx = state.canvasHeightPx,
                layerCount = state.layers.size,
                brushType = state.toolState.brushType,
                brushSizePx = state.toolState.brushSizePx,
                symmetryMode = state.toolState.symmetryMode,
                guideVisible = showCanvasGuides,
                onGuideToggle = { showCanvasGuides = !showCanvasGuides },
                modifier = Modifier.align(Alignment.TopCenter),
            )

            CanvasRenderSurface(
                bitmap = compositedBitmap,
                widthPx = state.canvasWidthPx,
                heightPx = state.canvasHeightPx,
                toolState = state.toolState,
                selection = state.selection,
                onStrokeComplete = { points, pressureWeights -> viewModel.drawManualStroke(points, pressureWeights) },
                onShapeComplete = viewModel::drawManualShape,
                onFillTap = viewModel::fillManualTap,
                onColorPickTap = viewModel::pickManualColor,
                onSelectionComplete = { left, top, right, bottom ->
                    viewModel.setSelection(
                        com.waheed.artificerx.domain.model.SelectionRect(left, top, right, bottom),
                    )
                },
                onTransformGestureStart = viewModel::beginTransformGesture,
                onTransformGesture = viewModel::transformActiveLayer,
                showGuides = showCanvasGuides,
            )

            // v0.4.30: appears only while a selection is active — Clear
            // deletes the pixels inside it (real transparency, see
            // CanvasCompositor.clearSelectionRegion), Deselect just drops
            // the marquee without touching pixels.
            if (state.selection != null) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 96.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    androidx.compose.material3.TextButton(onClick = { viewModel.setSelection(null) }) {
                        Text("Deselect")
                    }
                    androidx.compose.material3.TextButton(onClick = viewModel::clearSelectionContent) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

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
            brushType = state.toolState.brushType,
            opacity = state.toolState.brushOpacity,
            hardness = state.toolState.brushHardness,
            flow = state.toolState.brushFlow,
            spacing = state.toolState.brushSpacing,
            smoothing = state.toolState.brushSmoothing,
            scatter = state.toolState.brushScatter,
            sizePressure = state.toolState.brushSizePressure,
            opacityPressure = state.toolState.brushOpacityPressure,
            pressureSimulation = state.toolState.pressureSimulationEnabled,
            symmetry = state.toolState.symmetryMode,
            colorHex = state.toolState.brushColorHex,
            onToolSelected = viewModel::selectTool,
            onBrushSizeChanged = viewModel::setBrushSize,
            onBrushTypeSelected = viewModel::setBrushType,
            onBrushOpacityChanged = viewModel::setBrushOpacity,
            onBrushHardnessChanged = viewModel::setBrushHardness,
            onBrushFlowChanged = viewModel::setBrushFlow,
            onBrushSpacingChanged = viewModel::setBrushSpacing,
            onBrushSmoothingChanged = viewModel::setBrushSmoothing,
            onBrushScatterChanged = viewModel::setBrushScatter,
            onPressureResponseChanged = viewModel::setBrushPressureResponse,
            onPressureSimulationChanged = viewModel::setPressureSimulationEnabled,
            onSymmetryChanged = viewModel::setSymmetryMode,
            onColorChanged = viewModel::setBrushColor,
        )
    }
}

@Composable
private fun StudioTopBar(
    projectName: String,
    agentActivity: AgentActivityState,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleLayerPanel: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProjectGallery: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenSculptStudio: () -> Unit,
    onOpenTimelapse: () -> Unit,
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

        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(
                Icons.Filled.Undo,
                contentDescription = "Undo",
                tint = if (canUndo) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(
                Icons.Filled.Redo,
                contentDescription = "Redo",
                tint = if (canRedo) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onOpenExport) {
            Icon(Icons.Filled.Download, contentDescription = "Export", tint = GoldPrimary)
        }
        IconButton(onClick = onOpenSculptStudio) {
            Icon(Icons.Filled.ViewInAr, contentDescription = "3D Sculpt Studio", tint = GoldPrimary)
        }
        IconButton(onClick = onOpenTimelapse) {
            Icon(Icons.Filled.History, contentDescription = "Timelapse", tint = GoldPrimary)
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
    toolState: com.waheed.artificerx.domain.model.DrawToolState,
    selection: com.waheed.artificerx.domain.model.SelectionRect?,
    onStrokeComplete: (points: List<Float>, pressureWeights: List<Float>?) -> Unit,
    onShapeComplete: (startX: Float, startY: Float, endX: Float, endY: Float) -> Unit,
    onFillTap: (x: Float, y: Float) -> Unit,
    onColorPickTap: (x: Float, y: Float) -> Unit,
    onSelectionComplete: (left: Float, top: Float, right: Float, bottom: Float) -> Unit,
    onTransformGestureStart: () -> Unit,
    onTransformGesture: (dx: Float, dy: Float, scaleFactor: Float, rotationDegrees: Float, pivotX: Float, pivotY: Float) -> Unit,
    showGuides: Boolean = false,
) {
    // Live in-progress stroke/shape, in CANVAS BITMAP pixel space —
    // drawn as an immediate overlay so a finger drag shows visible
    // feedback before the gesture completes and the real compositor
    // write lands (see canvasTouchInput's own doc for why the actual
    // bitmap write itself stays a single call on release).
    var livePoints by remember { mutableStateOf<List<Float>?>(null) }
    var liveShapeBounds by remember { mutableStateOf<FloatArray?>(null) }
    // v0.4.30: same live-preview treatment for the selection marquee
    // during a drag — separate from liveShapeBounds so a rectangle
    // preview and a selection preview never fight over which one is
    // showing if a tool switch happens mid-drag.
    var liveSelectionBounds by remember { mutableStateOf<FloatArray?>(null) }

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
            androidx.compose.foundation.layout.BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                // The displayed Image uses ContentScale.Fit inside this
                // box, so it's letterboxed to the smaller of width/height
                // ratio — touch coordinates arrive in this Box's own
                // coordinate space and must be re-mapped into the
                // underlying bitmap's pixel space (which is what
                // CanvasCompositor/StudioViewModel operate in) before
                // being forwarded. Without this mapping, strokes land at
                // the wrong position (or entirely off-canvas) any time
                // the box's aspect ratio doesn't exactly match the
                // bitmap's — which is true almost always in practice.
                val boxWidthPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxWidth.toPx() }
                val boxHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { maxHeight.toPx() }
                val scale =
                    remember(boxWidthPx, boxHeightPx, widthPx, heightPx) {
                        minOf(boxWidthPx / widthPx.toFloat(), boxHeightPx / heightPx.toFloat())
                    }
                val displayedWidthPx = widthPx * scale
                val displayedHeightPx = heightPx * scale
                val offsetXPx = (boxWidthPx - displayedWidthPx) / 2f
                val offsetYPx = (boxHeightPx - displayedHeightPx) / 2f

                fun screenToCanvasX(x: Float): Float = ((x - offsetXPx) / scale).coerceIn(0f, widthPx.toFloat())

                fun screenToCanvasY(y: Float): Float = ((y - offsetYPx) / scale).coerceIn(0f, heightPx.toFloat())

                fun mapPoints(points: List<Float>): List<Float> =
                    points.mapIndexed { index, value ->
                        if (index % 2 == 0) screenToCanvasX(value) else screenToCanvasY(value)
                    }

                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Canvas artwork, $widthPx by $heightPx pixels",
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .canvasTouchInput(
                                toolState = toolState,
                                canvasSizePx = androidx.compose.ui.unit.IntSize(widthPx, heightPx),
                                onStrokeInProgress = { points -> livePoints = mapPoints(points) },
                                onStrokeComplete = { points, pressureWeights ->
                                    livePoints = null
                                    onStrokeComplete(mapPoints(points), pressureWeights)
                                },
                                onShapeInProgress = { sx, sy, ex, ey ->
                                    liveShapeBounds =
                                        floatArrayOf(screenToCanvasX(sx), screenToCanvasY(sy), screenToCanvasX(ex), screenToCanvasY(ey))
                                },
                                onShapeComplete = { sx, sy, ex, ey ->
                                    liveShapeBounds = null
                                    onShapeComplete(
                                        screenToCanvasX(sx),
                                        screenToCanvasY(sy),
                                        screenToCanvasX(ex),
                                        screenToCanvasY(ey),
                                    )
                                },
                                onFillTap = { x, y -> onFillTap(screenToCanvasX(x), screenToCanvasY(y)) },
                                onColorPickTap = { x, y -> onColorPickTap(screenToCanvasX(x), screenToCanvasY(y)) },
                                onSelectionInProgress = { sx, sy, ex, ey ->
                                    liveSelectionBounds =
                                        floatArrayOf(screenToCanvasX(sx), screenToCanvasY(sy), screenToCanvasX(ex), screenToCanvasY(ey))
                                },
                                onSelectionComplete = { sx, sy, ex, ey ->
                                    liveSelectionBounds = null
                                    onSelectionComplete(
                                        screenToCanvasX(sx),
                                        screenToCanvasY(sy),
                                        screenToCanvasX(ex),
                                        screenToCanvasY(ey),
                                    )
                                },
                                onTransformGestureStart = onTransformGestureStart,
                                // Compose's pan/zoom/rotation deltas arrive
                                // in the SCREEN coordinate space this
                                // Image occupies; dividing by `scale`
                                // converts pan distance into canvas-pixel
                                // distance (zoom/rotation are scale-
                                // invariant ratios/angles, so they pass
                                // through unchanged) before reaching the
                                // ViewModel, which operates purely in
                                // canvas-bitmap pixel space like every
                                // other tool here.
                                onTransformGesture = { dx, dy, zoom, rotation, pivotX, pivotY ->
                                    onTransformGesture(
                                        dx / scale,
                                        dy / scale,
                                        zoom,
                                        rotation,
                                        screenToCanvasX(pivotX),
                                        screenToCanvasY(pivotY),
                                    )
                                },
                            ),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                )

                // Live preview overlay — drawn in the same screen space the
                // Image occupies, converting the canvas-space live points
                // back to screen space for rendering only (the values
                // forwarded to the ViewModel above stay in canvas space).
                val previewPoints = livePoints
                val previewShape = liveShapeBounds
                // v0.4.30: persisted selection (from state) or the
                // in-progress drag preview, whichever is present —
                // rendered as a dashed-look marquee (gold, semi-
                // transparent fill) so it visibly reads as "selection"
                // rather than the solid-stroke rectangle-shape preview.
                val previewSelection = liveSelectionBounds ?: selection?.let { floatArrayOf(it.left, it.top, it.right, it.bottom) }
                if (previewPoints != null || previewShape != null || previewSelection != null) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeColor =
                            runCatching { android.graphics.Color.parseColor(toolState.brushColorHex) }
                                .map { androidx.compose.ui.graphics.Color(it) }
                                .getOrDefault(androidx.compose.ui.graphics.Color.White)
                        val strokeWidthDp = toolState.brushSizePx * scale

                        if (previewPoints != null && previewPoints.size >= 4) {
                            val path =
                                androidx.compose.ui.graphics.Path().apply {
                                    moveTo(previewPoints[0] * scale + offsetXPx, previewPoints[1] * scale + offsetYPx)
                                    var i = 2
                                    while (i + 1 < previewPoints.size) {
                                        lineTo(previewPoints[i] * scale + offsetXPx, previewPoints[i + 1] * scale + offsetYPx)
                                        i += 2
                                    }
                                }
                            drawPath(
                                path = path,
                                color = strokeColor,
                                style =
                                    androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidthDp,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round,
                                    ),
                            )
                        }

                        if (previewShape != null) {
                            val left = minOf(previewShape[0], previewShape[2]) * scale + offsetXPx
                            val top = minOf(previewShape[1], previewShape[3]) * scale + offsetYPx
                            val right = maxOf(previewShape[0], previewShape[2]) * scale + offsetXPx
                            val bottom = maxOf(previewShape[1], previewShape[3]) * scale + offsetYPx
                            drawRect(
                                color = strokeColor,
                                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidthDp),
                            )
                        }

                        if (previewSelection != null) {
                            val left = minOf(previewSelection[0], previewSelection[2]) * scale + offsetXPx
                            val top = minOf(previewSelection[1], previewSelection[3]) * scale + offsetYPx
                            val right = maxOf(previewSelection[0], previewSelection[2]) * scale + offsetXPx
                            val bottom = maxOf(previewSelection[1], previewSelection[3]) * scale + offsetYPx
                            drawRect(
                                color = GoldPrimary.copy(alpha = 0.15f),
                                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                            )
                            drawRect(
                                color = GoldPrimary,
                                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f),
                            )
                        }

                        if (showGuides) {
                            val gridColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.12f)
                            val count = 8
                            for (i in 1 until count) {
                                val gx = offsetXPx + (widthPx * i / count.toFloat()) * scale
                                val gy = offsetYPx + (heightPx * i / count.toFloat()) * scale
                                drawLine(gridColor, androidx.compose.ui.geometry.Offset(gx, offsetYPx), androidx.compose.ui.geometry.Offset(gx, offsetYPx + displayedHeightPx), strokeWidth = 1f)
                                drawLine(gridColor, androidx.compose.ui.geometry.Offset(offsetXPx, gy), androidx.compose.ui.geometry.Offset(offsetXPx + displayedWidthPx, gy), strokeWidth = 1f)
                            }
                            val centerColor = GoldPrimary.copy(alpha = 0.45f)
                            drawLine(centerColor, androidx.compose.ui.geometry.Offset(offsetXPx + displayedWidthPx / 2f, offsetYPx), androidx.compose.ui.geometry.Offset(offsetXPx + displayedWidthPx / 2f, offsetYPx + displayedHeightPx), strokeWidth = 1.5f)
                            drawLine(centerColor, androidx.compose.ui.geometry.Offset(offsetXPx, offsetYPx + displayedHeightPx / 2f), androidx.compose.ui.geometry.Offset(offsetXPx + displayedWidthPx, offsetYPx + displayedHeightPx / 2f), strokeWidth = 1.5f)
                        }
                    }
                }
            }
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
    brushType: com.waheed.artificerx.domain.model.BrushType,
    opacity: Float,
    hardness: Float,
    flow: Float,
    spacing: Float,
    smoothing: Float,
    scatter: Float,
    sizePressure: Float,
    opacityPressure: Float,
    pressureSimulation: Boolean,
    symmetry: com.waheed.artificerx.domain.model.SymmetryMode,
    colorHex: String,
    onToolSelected: (DrawToolType) -> Unit,
    onBrushSizeChanged: (Float) -> Unit,
    onBrushTypeSelected: (com.waheed.artificerx.domain.model.BrushType) -> Unit,
    onBrushOpacityChanged: (Float) -> Unit,
    onBrushHardnessChanged: (Float) -> Unit,
    onBrushFlowChanged: (Float) -> Unit,
    onBrushSpacingChanged: (Float) -> Unit,
    onBrushSmoothingChanged: (Float) -> Unit,
    onBrushScatterChanged: (Float) -> Unit,
    onPressureResponseChanged: (Float, Float) -> Unit,
    onPressureSimulationChanged: (Boolean) -> Unit,
    onSymmetryChanged: (com.waheed.artificerx.domain.model.SymmetryMode) -> Unit,
    onColorChanged: (String) -> Unit,
) {
    val tools = remember {
        listOf(
            DrawToolType.BRUSH to Icons.Filled.Brush,
            DrawToolType.ERASER to Icons.Filled.Delete,
            DrawToolType.SHAPE_RECT to Icons.Filled.Rectangle,
            DrawToolType.SHAPE_ELLIPSE to Icons.Filled.Circle,
            DrawToolType.GRADIENT to Icons.Filled.Gradient,
            DrawToolType.FILL to Icons.Filled.FormatColorFill,
            DrawToolType.SELECTION to Icons.Filled.Crop,
            DrawToolType.TRANSFORM to Icons.Filled.Transform,
            DrawToolType.TEXT to Icons.Filled.TextFields,
            DrawToolType.EYEDROPPER to Icons.Filled.ColorLens,
        )
    }
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
        ) {
            items(tools) { (tool, icon) -> ToolChip(icon = icon, isSelected = tool == activeTool, onClick = { onToolSelected(tool) }) }
        }
        if (activeTool == DrawToolType.BRUSH || activeTool == DrawToolType.ERASER) {
            AdvancedBrushDock(
                activeBrush = brushType,
                sizePx = brushSize,
                opacity = opacity,
                hardness = hardness,
                flow = flow,
                spacing = spacing,
                smoothing = smoothing,
                scatter = scatter,
                sizePressure = sizePressure,
                opacityPressure = opacityPressure,
                pressureSimulation = pressureSimulation,
                symmetry = symmetry,
                colorHex = colorHex,
                onBrushSelected = onBrushTypeSelected,
                onSizeChanged = onBrushSizeChanged,
                onOpacityChanged = onBrushOpacityChanged,
                onHardnessChanged = onBrushHardnessChanged,
                onFlowChanged = onBrushFlowChanged,
                onSpacingChanged = onBrushSpacingChanged,
                onSmoothingChanged = onBrushSmoothingChanged,
                onScatterChanged = onBrushScatterChanged,
                onPressureResponseChanged = onPressureResponseChanged,
                onPressureSimulationChanged = onPressureSimulationChanged,
                onSymmetryChanged = onSymmetryChanged,
                onColorChanged = onColorChanged,
            )
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
                        )
                        .then(Modifier)
                        .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = layer.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = "${layer.blendMode.name.lowercase()}  •  ${(layer.opacity * 100).toInt()}%" ,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = layer.opacity,
                        onValueChange = { onOpacityChange(layer.id, it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = GoldPrimary, activeTrackColor = GoldPrimary),
                    )
                }
                IconButton(onClick = { onToggleVisibility(layer.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.Circle,
                        contentDescription = "Toggle visibility",
                        tint = if (layer.isVisible) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
                IconButton(onClick = { onToggleLock(layer.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (layer.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = if (layer.isLocked) "Unlock layer" else "Lock layer",
                        tint = if (layer.isLocked) QualityFail else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
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
