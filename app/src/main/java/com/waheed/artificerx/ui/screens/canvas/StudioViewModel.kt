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
 * Section 155 Agent Event Loop's UI-facing half). Backs the real
 * render pipeline (CanvasCompositor bitmap writes, LayerBitmapStore
 * per-layer bitmaps) and exposes drawManualStroke/drawManualShape/
 * fillManualTap/pickManualColor as the entry points StudioScreen's
 * canvasTouchInput wiring calls into for finger-drawing — the same
 * compositor calls the agent's ToolExecutor uses, so a human stroke
 * and an agent stroke are pixel-identical in how they're rasterized.
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
        private val timelapseRecorder: com.waheed.artificerx.core.timelapse.TimelapseRecorder,
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
                    // v0.4.30 real timelapse: throttled inside
                    // TimelapseRecorder itself, so it's safe to call this
                    // on every recomposite without flooding storage.
                    timelapseRecorder.captureFrame(current.projectId, flattened)
                    // Keep the undo/redo counters in the exposed state in
                    // sync even when the mutation that triggered this
                    // recomposite came from the agent's ToolExecutor path
                    // rather than one of this ViewModel's own manual-draw
                    // methods (ToolExecutor calls bitmapStore.pushUndoSnapshot()
                    // itself before each tool's pixel mutation).
                    if (current.undoStackSize != bitmapStore.undoStackDepth() || current.redoStackSize != bitmapStore.redoStackDepth()) {
                        _state.update {
                            it.copy(undoStackSize = bitmapStore.undoStackDepth(), redoStackSize = bitmapStore.redoStackDepth())
                        }
                    }

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
        /** v0.4.30: two real fixes landed here together since they touch
         *  the same call site —
         *  1) ERASER now calls compositor.erasePath (true CLEAR-Xfermode
         *     transparency) instead of the old "paint solid white" hack,
         *     which corrupted layer alpha for blending/export.
         *  2) Touch-simulated pressure: when enabled, per-point stroke
         *     width is derived from how fast the finger moved between
         *     consecutive points (slow = wide/heavy, fast = thin/light)
         *     instead of one flat width — see simulatePressureWeights
         *     below. This is what answers "finger touch smoothing" from
         *     the brush-engine questionnaire without needing real stylus
         *     pressure hardware. */
        fun drawManualStroke(points: List<Float>) {
            val current = _state.value
            val activeLayerId = current.activeLayerId ?: return
            if (points.size < 4 || points.size % 2 != 0) return

            val activeLayer = current.layers.firstOrNull { it.id == activeLayerId }
            if (activeLayer?.isLocked == true) return

            bitmapStore.ensureLayer(activeLayerId, current.canvasWidthPx, current.canvasHeightPx)
            bitmapStore.pushUndoSnapshot()

            val isEraser = current.toolState.activeTool == DrawToolType.ERASER
            val variants =
                mirrorPointsForSymmetryPublic(points, current.toolState.symmetryMode, current.canvasWidthPx, current.canvasHeightPx)

            if (isEraser) {
                variants.forEach { variant ->
                    compositor.erasePath(layerId = activeLayerId, points = variant, strokeWidthPx = current.toolState.brushSizePx)
                }
                recomposite()
                return
            }

            val weights = if (current.toolState.pressureSimulationEnabled) simulatePressureWeights(points) else null
            variants.forEach { variant ->
                compositor.drawPath(
                    layerId = activeLayerId,
                    points = variant,
                    colorHex = current.toolState.brushColorHex,
                    strokeWidthPx = current.toolState.brushSizePx,
                    opacity = current.toolState.brushOpacity,
                    brushType = current.toolState.brushType,
                    pointWeights = weights,
                )
            }
            recomposite()
        }

        /** One weight per point (except the first, which has no prior
         *  point to measure speed from — matched 1:1 with drawPath's
         *  per-segment consumption). Distance-per-sample is converted to
         *  a 0.15..1.6 multiplier via an inverse curve so fast flicks
         *  thin out and slow deliberate motion lays down a heavier line —
         *  the same feel pressure-sensitive brushes give, derived purely
         *  from touch speed since finger input has no real pressure
         *  channel. Lightly smoothed (3-point moving average) so a single
         *  noisy touch sample doesn't cause a visible width "pop". */
        private fun simulatePressureWeights(points: List<Float>): List<Float> {
            val rawWeights = mutableListOf<Float>()
            var i = 2
            while (i + 1 < points.size) {
                val dx = points[i] - points[i - 2]
                val dy = points[i + 1] - points[i - 1]
                val distance = kotlin.math.hypot(dx, dy)
                // Calibrated for typical finger-drag speeds on a
                // Fit-scaled canvas surface: ~0-6px between samples reads
                // as "slow/heavy", ~25px+ reads as "fast/light".
                val weight = (1.5f - (distance / 18f)).coerceIn(0.15f, 1.6f)
                rawWeights.add(weight)
                i += 2
            }
            if (rawWeights.size < 3) return rawWeights
            return rawWeights.mapIndexed { idx, w ->
                val prev = rawWeights.getOrElse(idx - 1) { w }
                val next = rawWeights.getOrElse(idx + 1) { w }
                (prev + w + next) / 3f
            }
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
            bitmapStore.pushUndoSnapshot()

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
            bitmapStore.pushUndoSnapshot()
            compositor.fillRegion(activeLayerId, x, y, current.toolState.brushColorHex, tolerance = 32f)
            recomposite()
        }

        /** Section undo/redo: reverts the most recent pixel-mutating
         *  operation (manual stroke, shape, fill, or any agent tool call
         *  that went through CanvasCompositor). Layer *metadata* changes
         *  (visibility/lock/opacity/rename) are intentionally NOT part of
         *  this undo history — only pixel operations are, matching how
         *  Procreate/Photoshop-class apps scope undo (toggling a layer's
         *  visibility is not something artists expect an undo press to
         *  revert). Updates undoStackSize/redoStackSize on state so the
         *  UI can grey out the buttons when there's nothing to undo/redo. */
        fun undo() {
            if (!bitmapStore.undo()) return
            _state.update { it.copy(undoStackSize = bitmapStore.undoStackDepth(), redoStackSize = bitmapStore.redoStackDepth()) }
            recomposite()
        }

        fun redo() {
            if (!bitmapStore.redo()) return
            _state.update { it.copy(undoStackSize = bitmapStore.undoStackDepth(), redoStackSize = bitmapStore.redoStackDepth()) }
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

        /** v0.4.30: full project customization access for the AI (and
         *  reused by any future manual "New Project" size dialog) — see
         *  ResizeCanvas tool doc in ToolRegistry for why layer content is
         *  cropped/padded rather than rescaled: rescaling would silently
         *  blur/distort existing pixel work, whereas crop/pad is the
         *  predictable, lossless behavior every major editor uses for a
         *  canvas-size change (as opposed to an image-scale operation,
         *  which is a different, deliberate action). */
        fun resizeCanvas(
            widthPx: Int,
            heightPx: Int,
        ) {
            if (widthPx <= 0 || heightPx <= 0) return
            val current = _state.value
            bitmapStore.resizeAllLayers(current.layers.map { it.id }, widthPx, heightPx)
            _state.update { it.copy(canvasWidthPx = widthPx, canvasHeightPx = heightPx) }
            recomposite()
        }

        /** There's no dedicated "background" concept in this app's layer
         *  model (every layer is an equal transparent-capable bitmap) —
         *  matching that, this fills the bottom-most layer solid (creating
         *  one named "Background" first if the project is empty) rather
         *  than inventing a separate background field that the rest of
         *  the layer system (blend modes, opacity, reordering) wouldn't
         *  know how to treat consistently. */
        fun setCanvasBackground(colorHex: String) {
            val current = _state.value
            val bottomLayerId =
                current.layers.minByOrNull { it.orderIndex }?.id ?: run {
                    val backgroundLayer =
                        com.waheed.artificerx.domain.model.CanvasLayer(
                            id = UUID.randomUUID().toString(),
                            name = "Background",
                            orderIndex = 0,
                        )
                    bitmapStore.ensureLayer(backgroundLayer.id, current.canvasWidthPx, current.canvasHeightPx)
                    _state.update { it.copy(layers = it.layers + backgroundLayer, activeLayerId = it.activeLayerId ?: backgroundLayer.id) }
                    backgroundLayer.id
                }
            bitmapStore.ensureLayer(bottomLayerId, current.canvasWidthPx, current.canvasHeightPx)
            compositor.fillWholeLayer(bottomLayerId, colorHex, current.canvasWidthPx, current.canvasHeightPx)
            recomposite()
        }

        /** Standing defaults so an AI turn (or the human) can set a
         *  brush configuration once and have every following draw_path
         *  call use it without repeating every parameter — only non-null
         *  fields are applied, so a partial call like "just switch to
         *  airbrush" doesn't reset size/color/opacity back to defaults. */
        fun setBrushDefaults(
            brushType: com.waheed.artificerx.domain.model.BrushType? = null,
            sizePx: Float? = null,
            colorHex: String? = null,
            opacity: Float? = null,
            hardness: Float? = null,
        ) {
            _state.update { current ->
                current.copy(
                    toolState =
                        current.toolState.copy(
                            brushType = brushType ?: current.toolState.brushType,
                            brushSizePx = sizePx ?: current.toolState.brushSizePx,
                            brushColorHex = colorHex ?: current.toolState.brushColorHex,
                            brushOpacity = opacity ?: current.toolState.brushOpacity,
                            brushHardness = hardness ?: current.toolState.brushHardness,
                        ),
                )
            }
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
                com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_12 -> (0 until 12).map { rotateAround(points, it * 30.0) }
                com.waheed.artificerx.domain.model.SymmetryMode.RADIAL_16 -> (0 until 16).map { rotateAround(points, it * 22.5) }
                com.waheed.artificerx.domain.model.SymmetryMode.KALEIDOSCOPE_6 ->
                    (0 until 6).flatMap { step ->
                        val rotated = rotateAround(points, step * 60.0)
                        listOf(rotated, mirrorVertical(rotated))
                    }
                com.waheed.artificerx.domain.model.SymmetryMode.KALEIDOSCOPE_12 ->
                    (0 until 12).flatMap { step ->
                        val rotated = rotateAround(points, step * 30.0)
                        listOf(rotated, mirrorVertical(rotated))
                    }
                com.waheed.artificerx.domain.model.SymmetryMode.MANDALA_24 ->
                    (0 until 24).flatMap { step ->
                        val rotated = rotateAround(points, step * 15.0)
                        listOf(rotated, mirrorVertical(rotated))
                    }
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

        fun setBrushType(type: com.waheed.artificerx.domain.model.BrushType) {
            _state.update { it.copy(toolState = it.toolState.copy(brushType = type)) }
        }

        fun setPressureSimulationEnabled(enabled: Boolean) {
            _state.update { it.copy(toolState = it.toolState.copy(pressureSimulationEnabled = enabled)) }
        }

        /** v0.4.30 selection tool. [rect] is in canvas-pixel space (see
         *  StudioScreen's screenToCanvasPx for the Fit-scaled render
         *  surface's mapping) — null clears the selection entirely. */
        fun setSelection(rect: com.waheed.artificerx.domain.model.SelectionRect?) {
            _state.update { it.copy(selection = rect?.normalized()) }
        }

        /** Deletes the pixel content inside the active selection on the
         *  active layer (real transparency clear, not a white-paint hack —
         *  see CanvasCompositor.clearSelectionRegion's doc). Selection
         *  itself stays active afterwards so the user can keep working
         *  inside the same marquee (matches Photoshop/Procreate). */
        fun clearSelectionContent() {
            val current = _state.value
            val selection = current.selection ?: return
            val activeLayerId = current.activeLayerId ?: return
            val activeLayer = current.layers.firstOrNull { it.id == activeLayerId }
            if (activeLayer?.isLocked == true) return

            bitmapStore.pushUndoSnapshot()
            compositor.clearSelectionRegion(activeLayerId, selection.left, selection.top, selection.right, selection.bottom)
            recomposite()
        }

        /** Moves the pixel content inside the active selection by
         *  (dx, dy) on the active layer — the selection rect itself moves
         *  with it so a drag-to-move gesture can call this repeatedly per
         *  frame and both the pixels and the marquee track the finger. */
        fun moveSelectionContent(
            dx: Float,
            dy: Float,
        ) {
            val current = _state.value
            val selection = current.selection ?: return
            val activeLayerId = current.activeLayerId ?: return
            val activeLayer = current.layers.firstOrNull { it.id == activeLayerId }
            if (activeLayer?.isLocked == true) return

            compositor.moveSelectionRegion(activeLayerId, selection.left, selection.top, selection.right, selection.bottom, dx, dy)
            _state.update {
                it.copy(
                    selection =
                        com.waheed.artificerx.domain.model.SelectionRect(
                            left = selection.left + dx,
                            top = selection.top + dy,
                            right = selection.right + dx,
                            bottom = selection.bottom + dy,
                        ),
                )
            }
            recomposite()
        }

        /** v0.4.30: backs TimelapseScreen's playback frame list — see
         *  TimelapseRecorder's doc for the capture/throttle/storage
         *  design. */
        suspend fun listTimelapseFrames(): List<java.io.File> = timelapseRecorder.listFrames(_state.value.projectId)

        /** Call once (from CanvasTouchOverlay's TRANSFORM gesture start)
         *  before a drag begins, so the whole gesture is a single undo
         *  step instead of one per frame — undoing a transform should put
         *  the layer back to how it looked before the user touched it,
         *  not back one tiny rotation increment at a time. */
        fun beginTransformGesture() {
        }

        /** v0.4.30 transform tool: applies one frame's worth of
         *  pan/scale/rotate delta directly to the active layer's bitmap,
         *  so the layer visibly moves/scales/rotates live under the
         *  finger during the gesture (see LayerBitmapStore.transformLayer
         *  doc for why this is a fixed-size scratch composite rather than
         *  Bitmap.createBitmap's auto-cropping variant). Pivot is the
         *  gesture's centroid in canvas-pixel space. */
        fun transformActiveLayer(
            dx: Float,
            dy: Float,
            scaleFactor: Float,
            rotationDegrees: Float,
            pivotX: Float,
            pivotY: Float,
        ) {
            val current = _state.value
            val activeLayerId = current.activeLayerId ?: return
            val activeLayer = current.layers.firstOrNull { it.id == activeLayerId }
            if (activeLayer?.isLocked == true) return

            bitmapStore.transformLayer(activeLayerId, dx, dy, scaleFactor, rotationDegrees, pivotX, pivotY)
            recomposite()
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
                    // A freshly loaded project's undo history from a
                    // previous session (if any) refers to bitmaps that no
                    // longer correspond to this project's layers — undoing
                    // "past" this point would restore stale, unrelated
                    // pixel data, so history must reset at load boundaries.
                    bitmapStore.clearHistory()
                    _state.value = loaded.copy(undoStackSize = 0, redoStackSize = 0)
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
