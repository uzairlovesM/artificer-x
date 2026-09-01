package com.waheed.artificerx.ui.screens.sculpt

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

        /** No renderer attached yet — returns null so AgentOrchestrator's
         *  vision-feedback loop simply skips the snapshot step for 3D
         *  turns until a real GPU renderer is wired in, rather than
         *  crashing on a missing dependency. */
        fun captureSnapshotNow(): android.graphics.Bitmap? = null
    }
