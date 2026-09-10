package com.waheed.artificerx.ui.screens.sculpt

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.mesh.PrimitiveMeshGenerator
import com.waheed.artificerx.core.mesh.SculptBrushEngine
import com.waheed.artificerx.core.mesh.SculptSceneStore
import com.waheed.artificerx.domain.model.AgentActivityState
import com.waheed.artificerx.domain.model.PrimitiveType
import com.waheed.artificerx.domain.model.SculptBrushType
import com.waheed.artificerx.domain.model.SculptMesh
import com.waheed.artificerx.domain.model.Vec3
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SculptUiState(
    val meshes: Map<String, SculptMesh> = emptyMap(),
    val activeMeshId: String? = null,
    val activeBrush: SculptBrushType = SculptBrushType.PULL,
    val brushRadius: Float = 0.3f,
    val brushStrength: Float = 0.5f,
    val agentActivity: AgentActivityState = AgentActivityState.IDLE,
)

/**
 * Owns the interactive (manual, non-agent) sculpting session — direct
 * screen-touch sculpting mirrors StudioViewModel's role for 2D, while
 * SculptToolExecutor (core.agent-adjacent) handles agent-driven
 * sculpting through tool_calls. Both paths converge on the same
 * SculptSceneStore, so a manual stroke and an agent stroke are
 * indistinguishable to the renderer — exactly like 2D's StudioViewModel/
 * ToolExecutor split around LayerBitmapStore.
 *
 * Renderer-agnostic — owns mesh editing state only. A GPU renderer
 * (GLES or otherwise) attaches later purely as a reader of
 * SculptSceneStore; this ViewModel and everything below it stays
 * unchanged whichever renderer gets wired in.
 */
@HiltViewModel
class SculptViewModel
    @Inject
    constructor(
        private val sceneStore: SculptSceneStore,
        private val primitiveMeshGenerator: PrimitiveMeshGenerator,
        private val sculptBrushEngine: SculptBrushEngine,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SculptUiState())
        val uiState: StateFlow<SculptUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                sceneStore.meshes.collect { meshes ->
                    _uiState.update { it.copy(meshes = meshes) }
                }
            }
        }

        fun addPrimitive(type: PrimitiveType) {
            val mesh =
                primitiveMeshGenerator.generate(
                    type,
                    "${type.name.lowercase().replaceFirstChar { it.uppercase() }} ${_uiState.value.meshes.size + 1}",
                )
            sceneStore.addMesh(mesh)
            _uiState.update { it.copy(activeMeshId = mesh.id) }
        }

        fun setActiveMesh(meshId: String) {
            _uiState.update { it.copy(activeMeshId = meshId) }
        }

        fun deleteMesh(meshId: String) {
            sceneStore.removeMesh(meshId)
            _uiState.update {
                it.copy(activeMeshId = if (it.activeMeshId == meshId) null else it.activeMeshId)
            }
        }

        fun selectBrush(brush: SculptBrushType) {
            _uiState.update { it.copy(activeBrush = brush) }
        }

        fun setBrushRadius(radius: Float) {
            _uiState.update { it.copy(brushRadius = radius.coerceIn(0.05f, 2f)) }
        }

        fun setBrushStrength(strength: Float) {
            _uiState.update { it.copy(brushStrength = strength.coerceIn(0.05f, 2f)) }
        }

        /** Called from the SurfaceView's touch handler once a screen-space
         *  drag has been ray-cast into a 3D world-space hit point (the
         *  raycast itself happens in SculptSurfaceView using the camera's
         *  current view-projection matrix, since that math needs to live
         *  next to Filament's live camera state). */
        fun applyManualStroke(hitPoint: Vec3) {
            val meshId = _uiState.value.activeMeshId ?: return
            val mesh = sceneStore.getMesh(meshId) ?: return
            val state = _uiState.value

            val sculpted = sculptBrushEngine.applyStroke(mesh, state.activeBrush, hitPoint, state.brushRadius, state.brushStrength)
            sceneStore.updateMesh(sculpted)
        }

        fun setAgentActivity(activity: AgentActivityState) {
            _uiState.update { it.copy(agentActivity = activity) }
        }

        /** Produces a deterministic CPU mesh thumbnail for AI inspection and
         *  persistence even when the live GPU viewport is not attached. */
        fun captureSnapshotNow(): Bitmap? {
            val meshes = sceneStore.meshes.value.values
            if (meshes.isEmpty()) return null
            val bitmap = Bitmap.createBitmap(768, 768, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.rgb(20, 20, 24))
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 2.5f
            }
            val allVertices = meshes.flatMap { it.vertices }
            val minX = allVertices.minOf { it.x }
            val maxX = allVertices.maxOf { it.x }
            val minY = allVertices.minOf { it.y }
            val maxY = allVertices.maxOf { it.y }
            val span = maxOf(maxX - minX, maxY - minY, 0.001f)
            val scale = 640f / span
            fun sx(x: Float) = 64f + (x - minX) * scale
            fun sy(y: Float) = 64f + (maxY - y) * scale
            meshes.forEach { mesh ->
                paint.color = runCatching { android.graphics.Color.parseColor(mesh.colorHex) }.getOrDefault(android.graphics.Color.WHITE)
                mesh.triangleIndices.chunked(3).forEach { tri ->
                    if (tri.size == 3 && tri.all { it in mesh.vertices.indices }) {
                        val a = mesh.vertices[tri[0]]
                        val b = mesh.vertices[tri[1]]
                        val c = mesh.vertices[tri[2]]
                        canvas.drawLine(sx(a.x), sy(a.y), sx(b.x), sy(b.y), paint)
                        canvas.drawLine(sx(b.x), sy(b.y), sx(c.x), sy(c.y), paint)
                        canvas.drawLine(sx(c.x), sy(c.y), sx(a.x), sy(a.y), paint)
                    }
                }
            }
            return bitmap
        }
    }
