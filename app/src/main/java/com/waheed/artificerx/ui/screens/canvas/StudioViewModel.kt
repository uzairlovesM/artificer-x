package com.waheed.artificerx.ui.screens.canvas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.security.EncryptedKeyStore
import com.waheed.artificerx.domain.model.AgentActivityState
import com.waheed.artificerx.domain.model.CanvasLayer
import com.waheed.artificerx.domain.model.CanvasProjectState
import com.waheed.artificerx.domain.model.DrawToolType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Owns the active canvas/project session (Section 26 Layer System,
 * Section 155 Agent Event Loop's UI-facing half). The real render
 * pipeline (bitmap compositing, undo/redo command stack, tool
 * dispatch to the agent's tool registry) lands in later phases —
 * this ViewModel establishes the state shape and layer-management
 * operations that survive unchanged once rendering is wired in,
 * so the Studio UI never needs rework, only its canvas surface gets
 * a real implementation swapped in.
 *
 * Registers a crash-safe flush callback (Section 147) immediately on
 * creation so unsaved layer state is captured if the process dies
 * mid-session.
 */
@HiltViewModel
class StudioViewModel
    @Inject
    constructor(
        private val keyStore: EncryptedKeyStore,
        private val projectRepository: com.waheed.artificerx.data.repository.ProjectRepository,
        private val bitmapStore: com.waheed.artificerx.core.render.LayerBitmapStore,
        private val compositor: com.waheed.artificerx.core.render.CanvasCompositor,
    ) : ViewModel() {
        private val _compositedBitmap = kotlinx.coroutines.flow.MutableStateFlow<android.graphics.Bitmap?>(null)
        val compositedBitmap: kotlinx.coroutines.flow.StateFlow<android.graphics.Bitmap?> = _compositedBitmap.asStateFlow()

        private var recompositeJob: kotlinx.coroutines.Job? = null
        private var pendingAttachedImage: android.graphics.Bitmap? = null

        private val _state =
            MutableStateFlow(
                CanvasProjectState(
                    projectId = UUID.randomUUID().toString(),
                    projectName = "Untitled",
                    layers =
                        listOf(
                            CanvasLayer(id = UUID.randomUUID().toString(), name = "Background", orderIndex = 0),
                        ),
                    activeLayerId = null,
                ),
            )
        val state: StateFlow<CanvasProjectState> = _state.asStateFlow()

        private var autoSaveJob: kotlinx.coroutines.Job? = null
        private var lastSavedStateHash: Int = 0

        init {
            _state.update { it.copy(activeLayerId = it.layers.firstOrNull()?.id) }
            val initialLayerId = _state.value.activeLayerId
            if (initialLayerId != null) {
                bitmapStore.ensureLayer(initialLayerId, _state.value.canvasWidthPx, _state.value.canvasHeightPx)
            }
            com.waheed.artificerx.CrashSafeSaveRegistry
                .register(::flushToDisk)
            recomposite()
            startPeriodicAutoSave()
        }

        /** Section 147 crash-safe save's proactive half: rather than only
         *  saving when the process is about to die, save on a steady
         *  cadence whenever the project state has actually changed since
         *  the last save — cheap because it's a structural equality check
         *  (layers list hash), not a full bitmap diff. Runs for the
         *  lifetime of this ViewModel (i.e. while the Studio/Chat screens
         *  are on the back stack), cancelled in onCleared(). */
        private fun startPeriodicAutoSave() {
            autoSaveJob?.cancel()
            autoSaveJob =
                viewModelScope.launch {
                    while (true) {
                        kotlinx.coroutines.delay(AUTO_SAVE_INTERVAL_MS)
                        val current = _state.value
                        val currentHash = current.layers.hashCode() * 31 + current.projectName.hashCode()
                        if (currentHash != lastSavedStateHash) {
                            runCatching {
                                projectRepository.saveCurrentState(current)
                                lastSavedStateHash = currentHash
                            }
                        }
                    }
                }
        }

        /** Re-flattens all visible layers into one displayable bitmap.
         *  Called after every tool execution that touches pixels — debounced
         *  slightly via a cancel-and-relaunch coroutine so a burst of rapid
         *  tool calls (a multi-stroke turn) doesn't recomposite on every
         *  single call, matching Section 137's low-end-device performance
         *  budget. */
        fun recomposite() {
            recompositeJob?.cancel()
            recompositeJob =
                viewModelScope.launch {
                    kotlinx.coroutines.delay(RECOMPOSITE_DEBOUNCE_MS)
                    val current = _state.value
                    val flattened =
                        compositor.compositeVisibleLayers(
                            layers = current.layers,
                            widthPx = current.canvasWidthPx,
                            heightPx = current.canvasHeightPx,
                        )
                    _compositedBitmap.value = flattened

                    // Debounced save-on-change: any tool call or manual edit that
                    // triggers a recomposite also schedules a save shortly after,
                    // so agent-driven sessions are captured without waiting for
                    // the full periodic interval.
                    val currentHash = current.layers.hashCode() * 31 + current.projectName.hashCode()
                    if (currentHash != lastSavedStateHash) {
                        runCatching {
                            projectRepository.saveCurrentState(current)
                            lastSavedStateHash = currentHash
                        }
                    }
                }
        }

        fun captureSnapshotNow(): android.graphics.Bitmap {
            val current = _state.value
            return compositor.compositeVisibleLayers(
                layers = current.layers,
                widthPx = current.canvasWidthPx,
                heightPx = current.canvasHeightPx,
            )
        }

        fun addLayer() {
            _state.update { current ->
                val newLayer =
                    CanvasLayer(
                        id = UUID.randomUUID().toString(),
                        name = "Layer ${current.layers.size + 1}",
                        orderIndex = current.layers.size,
                    )
                bitmapStore.ensureLayer(newLayer.id, current.canvasWidthPx, current.canvasHeightPx)
                current.copy(layers = current.layers + newLayer, activeLayerId = newLayer.id)
            }
            recomposite()
        }

        /** Backs the duplicate_layer tool — copies both the pixel content
         *  (via LayerBitmapStore.duplicateLayer, a real pixel-for-pixel
         *  Bitmap.copy) and the layer metadata (opacity, blend mode,
         *  visibility) of an existing layer, inserting the copy directly
         *  above the source and making it active — matching how most
         *  paint tools place a fresh duplicate. Returns false if the
         *  source layer doesn't exist, so ToolExecutor can surface a clear
         *  error instead of silently no-op'ing. */
        fun duplicateLayer(
            sourceLayerId: String,
            newName: String?,
        ): Boolean {
            val current = _state.value
            val sourceLayer = current.layers.firstOrNull { it.id == sourceLayerId } ?: return false
            val newLayerId = UUID.randomUUID().toString()
            if (!bitmapStore.duplicateLayer(sourceLayerId, newLayerId)) return false

            val sourceIndex = current.layers.indexOf(sourceLayer)
            val newLayer =
                sourceLayer.copy(
                    id = newLayerId,
                    name = newName ?: "${sourceLayer.name} copy",
                    orderIndex = sourceLayer.orderIndex + 1,
                )
            val updatedLayers =
                current.layers
                    .toMutableList()
                    .apply {
                        add(sourceIndex + 1, newLayer)
                    }.mapIndexed { index, layer -> layer.copy(orderIndex = index) }

            _state.update { it.copy(layers = updatedLayers, activeLayerId = newLayerId) }
            recomposite()
            return true
        }

        /** Backs the flip_layer tool — mirrors a layer's pixel content in
         *  place. Metadata (name, opacity, blend mode) is untouched since
         *  flipping is a pixel operation only. */
        fun flipLayer(
            layerId: String,
            horizontal: Boolean,
            vertical: Boolean,
        ): Boolean {
            if (_state.value.layers.none { it.id == layerId }) return false
            val success = bitmapStore.flipLayer(layerId, horizontal, vertical)
            if (success) recomposite()
            return success
        }

        /** Backs the crop_canvas tool. Crops every layer's bitmap to the
         *  same new region and shrinks the canvas dimensions to match —
         *  every layer must always share the canvas's exact pixel
         *  dimensions, so this can't be a per-layer operation the way
         *  duplicate/flip are. */
        fun cropCanvas(
            cropX: Int,
            cropY: Int,
            cropWidth: Int,
            cropHeight: Int,
        ): Boolean {
            val current = _state.value
            if (cropWidth <= 0 || cropHeight <= 0) return false

            val allSucceeded =
                current.layers.all { layer ->
                    bitmapStore.cropLayer(layer.id, cropX, cropY, cropWidth, cropHeight)
                }
            if (!allSucceeded) return false

            val newWidth = cropWidth.coerceAtMost(current.canvasWidthPx - cropX).coerceAtLeast(1)
            val newHeight = cropHeight.coerceAtMost(current.canvasHeightPx - cropY).coerceAtLeast(1)
            _state.update { it.copy(canvasWidthPx = newWidth, canvasHeightPx = newHeight) }
            recomposite()
            return true
        }

        fun deleteLayer(layerId: String) {
            _state.update { current ->
                if (current.layers.size <= 1) return@update current
                val remaining = current.layers.filterNot { it.id == layerId }
                val newActive = if (current.activeLayerId == layerId) remaining.lastOrNull()?.id else current.activeLayerId
                current.copy(layers = remaining, activeLayerId = newActive)
            }
        }

        fun setActiveLayer(layerId: String) {
            _state.update { it.copy(activeLayerId = layerId) }
        }

        fun toggleLayerVisibility(layerId: String) {
            _state.update { current ->
                current.copy(
                    layers =
                        current.layers.map {
                            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
                        },
                )
            }

            recomposite()
        }

        fun toggleLayerLock(layerId: String) {
            _state.update { current ->
                current.copy(
                    layers =
                        current.layers.map {
                            if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
                        },
                )
            }
        }

        fun setLayerOpacity(
            layerId: String,
            opacity: Float,
        ) {
            _state.update { current ->
                current.copy(
                    layers =
                        current.layers.map {
                            if (it.id == layerId) it.copy(opacity = opacity.coerceIn(0f, 1f)) else it
                        },
                )
            }

            recomposite()
        }

        /** Real manual finger-drawing entry point — mirrors ToolExecutor's
         *  DrawPath branch exactly (same CanvasCompositor call, same
         *  symmetry-mirroring behavior) so a human stroke and an agent
         *  stroke are indistinguishable in the resulting pixels. Called
         *  from CanvasTouchOverlay once a gesture completes. */
        fun drawManualStroke(points: List<Float>) {
            val current = _state.value
            val activeLayerId = current.activeLayerId ?: return
            if (points.size < 4 || points.size % 2 != 0) return

            val activeLayer = current.layers.firstOrNull { it.id == activeLayerId }
            if (activeLayer?.isLocked == true) return

            bitmapStore.ensureLayer(activeLayerId, current.canvasWidthPx, current.canvasHeightPx)

            val isEraser = current.toolState.activeTool == DrawToolType.ERASER
            val colorHex = if (isEraser) "#FFFFFF" else current.toolState.brushColorHex

            val variants =
                mirrorPointsForSymmetryPublic(points, current.toolState.symmetryMode, current.canvasWidthPx, current.canvasHeightPx)
            variants.forEach { variant ->
                compositor.drawPath(
                    layerId = activeLayerId,
                    points = variant,
                    colorHex = colorHex,
                    strokeWidthPx = current.toolState.brushSizePx,
                    opacity = current.toolState.brushOpacity,
                )
            }
            recomposite()
        }

        fun drawManualShape(
            startX: Float,
            startY: Float,
            endX: Float,
            endY: Float,
        ) {
            val current = _state.value
            val activeLayerId = current.activeLayerId ?: return
            val activeLayer = current.layers.firstOrNull { it.id == activeLayerId }
            if (activeLayer?.isLocked == true) return

            bitmapStore.ensureLayer(activeLayerId, current.canvasWidthPx, current.canvasHeightPx)

            val shapeType =
                when (current.toolState.activeTool) {
                    DrawToolType.SHAPE_ELLIPSE -> "ellipse"
                    DrawToolType.SHAPE_LINE -> "line"
                    else -> "rectangle"
                }
            val colorHex = current.toolState.brushColorHex

            compositor.drawShape(
                layerId = activeLayerId,
                shapeType = shapeType,
                x = minOf(startX, endX),
                y = minOf(startY, endY),
                width = kotlin.math.abs(endX - startX),
                height = kotlin.math.abs(endY - startY),
                fillColorHex = if (shapeType != "line") colorHex else null,
                strokeColorHex = colorHex,
                strokeWidthPx = current.toolState.brushSizePx,
            )
            recomposite()
        }

        fun fillManualTap(
            x: Float,
            y: Float,
        ) {
            val current = _state.value
            val activeLayerId = current.activeLayerId ?: return
            val activeLayer = current.layers.firstOrNull { it.id == activeLayerId }
            if (activeLayer?.isLocked == true) return

            bitmapStore.ensureLayer(activeLayerId, current.canvasWidthPx, current.canvasHeightPx)
            compositor.fillRegion(activeLayerId, x, y, current.toolState.brushColorHex, tolerance = 32f)
            recomposite()
        }

        private val _pickedColorHex = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
        val pickedColorHex: kotlinx.coroutines.flow.StateFlow<String?> = _pickedColorHex.asStateFlow()

        fun pickManualColor(
            x: Float,
            y: Float,
        ) {
            val snapshot = captureSnapshotNow()
            val hex = compositor.pickColor(snapshot, x, y)
            _pickedColorHex.value = hex
            setBrushColor(hex)
        }

        fun setBrushColor(colorHex: String) {
            val isValid = runCatching { android.graphics.Color.parseColor(colorHex) }.isSuccess
            if (!isValid) return
            _state.update { it.copy(toolState = it.toolState.copy(brushColorHex = colorHex)) }
        }

        /** Public wrapper so both manual touch input and ToolExecutor's
         *  agent path can reuse identical symmetry-mirroring math without
         *  duplicating it — ToolExecutor keeps its own copy for now since
         *  it operates on ParsedToolCall types, but both implementations
         *  are kept in lockstep intentionally. */
        private fun mirrorPointsForSymmetryPublic(
            points: List<Float>,
            mode: com.waheed.artificerx.domain.model.SymmetryMode,
            canvasWidth: Int,
            canvasHeight: Int,
        ): List<List<Float>> {
            if (mode == com.waheed.artificerx.domain.model.SymmetryMode.OFF) return listOf(points)

            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            fun mirrorVertical(pts: List<Float>) = pts.mapIndexed { i, v -> if (i % 2 == 0) (2 * centerX - v) else v }

            fun mirrorHorizontal(pts: List<Float>) = pts.mapIndexed { i, v -> if (i % 2 == 1) (2 * centerY - v) else v }

            fun rotateAround(
                pts: List<Float>,
                degrees: Double,
            ): List<Float> {
                val radians = Math.toRadians(degrees)
                val cos = Math.cos(radians).toFloat()
                val sin = Math.sin(radians).toFloat()
                val result = mutableListOf<Float>()
                var i = 0
                while (i + 1 < pts.size) {
                    val dx = pts[i] - centerX
                    val dy = pts[i + 1] - centerY
                    result.add(centerX + dx * cos - dy * sin)
                    result.add(centerY + dx * sin + dy * cos)
                    i += 2
                }
                return result
            }

            return when (mode) {
                com.waheed.artificerx.domain.model.SymmetryMode.VERTICAL -> listOf(points, mirrorVertical(points))
                com.waheed.artificerx.domain.model.SymmetryMode.HORIZONTAL -> listOf(points, mirrorHorizontal(points))
                com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_4 -> listOf(0.0, 90.0, 180.0, 270.0).map { rotateAround(points, it) }
                com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_8 -> (0 until 8).map { rotateAround(points, it * 45.0) }
                com.waheed.artificerx.domain.model.SymmetryMode.OFF -> listOf(points)
            }
        }

        fun selectTool(tool: DrawToolType) {
            _state.update { it.copy(toolState = it.toolState.copy(activeTool = tool)) }
        }

        /** Called by AgentChatViewModel right before dispatching a user
         *  message that has an attached reference image, so ToolExecutor's
         *  import_image_layer tool has real bitmap data to work with when
         *  the agent decides to call it. Cleared after one consumption so
         *  a stale image never silently gets reused on an unrelated later
         *  turn. */
        fun setPendingAttachedImage(bitmap: android.graphics.Bitmap?) {
            pendingAttachedImage = bitmap
        }

        fun consumePendingAttachedImage(): android.graphics.Bitmap? {
            val bitmap = pendingAttachedImage
            pendingAttachedImage = null
            return bitmap
        }

        fun setSymmetryMode(mode: com.waheed.artificerx.domain.model.SymmetryMode) {
            _state.update { it.copy(toolState = it.toolState.copy(symmetryMode = mode)) }
        }

        fun setBrushSize(sizePx: Float) {
            _state.update { it.copy(toolState = it.toolState.copy(brushSizePx = sizePx)) }
        }

        fun setAgentActivity(activity: AgentActivityState) {
            _state.update { it.copy(agentActivity = activity) }
        }

        private fun flushToDisk() {
            // Runs on the crash path (uncaught exception handler) which may
            // not have a live coroutine scope by the time it fires, so this
            // blocks synchronously on IO rather than launching — Section 147
            // crash-safe save must complete before the process dies, not
            // "eventually."
            runCatching {
                projectRepository.saveCurrentStateBlocking(_state.value)
            }
        }

        fun saveNow() {
            viewModelScope.launch {
                projectRepository.saveCurrentState(_state.value)
            }
        }

        fun createVersionCheckpoint(
            label: String,
            triggeredBy: String = "manual",
        ) {
            viewModelScope.launch {
                projectRepository.saveCurrentState(_state.value)
                projectRepository.createVersionCheckpoint(_state.value, triggeredBy, label)
            }
        }

        fun loadProject(projectId: String) {
            viewModelScope.launch {
                projectRepository.loadProject(projectId)?.let { loaded ->
                    _state.value = loaded
                }
            }
        }

        override fun onCleared() {
            autoSaveJob?.cancel()
            com.waheed.artificerx.CrashSafeSaveRegistry
                .unregister(::flushToDisk)
            // NOTE: no blocking save here. onCleared() is a normal lifecycle
            // event (navigating away), not a crash — the periodic auto-save
            // (every 30s) and debounced save-on-recomposite already cover
            // this case without risking a main-thread runBlocking stall.
            // The true crash path is CrashSafeSaveRegistry -> flushToDisk,
            // invoked from ArtificerXApp's uncaught exception handler, which
            // is the only place a synchronous blocking save is justified.
            super.onCleared()
        }

        companion object {
            private const val RECOMPOSITE_DEBOUNCE_MS = 80L
            private const val AUTO_SAVE_INTERVAL_MS = 30_000L
        }
    }
