package com.waheed.artificerx.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LayerBlendMode {
    NORMAL,
    MULTIPLY,
    SCREEN,
    OVERLAY,
    DARKEN,
    LIGHTEN,
    COLOR_DODGE,
    COLOR_BURN,
    ADD,
    SUBTRACT,
}

@Serializable
data class CanvasLayer(
    val id: String,
    val name: String,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val opacity: Float = 1f,
    val blendMode: LayerBlendMode = LayerBlendMode.NORMAL,
    val orderIndex: Int,
)

enum class DrawToolType {
    BRUSH,
    ERASER,
    SHAPE_RECT,
    SHAPE_ELLIPSE,
    SHAPE_LINE,
    GRADIENT,
    FILL,
    SELECTION,
    TEXT,
    EYEDROPPER,
}

enum class SymmetryMode { OFF, VERTICAL, HORIZONTAL, RADIAL_4, RADIAL_8 }

data class DrawToolState(
    val activeTool: DrawToolType = DrawToolType.BRUSH,
    val brushSizePx: Float = 8f,
    val brushColorHex: String = "#FFD700",
    val brushOpacity: Float = 1f,
    val brushHardness: Float = 0.8f,
    val symmetryMode: SymmetryMode = SymmetryMode.OFF,
)

enum class AgentActivityState {
    IDLE,
    THINKING,
    CALLING_TOOL,
    RENDERING,
    SELF_CORRECTING,
    AWAITING_USER_INPUT,
    ERROR,
}

data class CanvasProjectState(
    val projectId: String,
    val projectName: String,
    val layers: List<CanvasLayer> = emptyList(),
    val activeLayerId: String? = null,
    val toolState: DrawToolState = DrawToolState(),
    val agentActivity: AgentActivityState = AgentActivityState.IDLE,
    val canvasWidthPx: Int = 1024,
    val canvasHeightPx: Int = 1024,
    val undoStackSize: Int = 0,
    val redoStackSize: Int = 0,
)
