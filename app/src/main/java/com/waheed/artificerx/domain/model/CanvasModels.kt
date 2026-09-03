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
    TRANSFORM,
    TEXT,
    EYEDROPPER,
}

enum class SymmetryMode { OFF, VERTICAL, HORIZONTAL, RADIAL_4, RADIAL_8, RADIAL_12, RADIAL_16, KALEIDOSCOPE_6, KALEIDOSCOPE_12, MANDALA_24 }

/** v0.4.30: real brush engine — each type renders with genuinely
 *  different Paint/mask-filter behavior in CanvasCompositor.drawPath,
 *  not just a label. Shared by both the manual finger-touch path and
 *  the AI's draw_path tool calls, so a human stroke and an agent
 *  stroke drawn with the same brush type look the same. */
enum class BrushType {
    PENCIL,
    INK_PEN,
    MARKER,
    CALLIGRAPHY,
    AIRBRUSH,
    CHARCOAL,
    WATERCOLOR,
    ERASER_SOFT,
}

/** v0.4.30: axis-aligned selection in canvas-pixel space. Null means
 *  "no active selection" (the whole layer is implicitly the working
 *  area, same as before this existed). Coordinates are always stored
 *  normalized (left<=right, top<=bottom) so downstream Rect math never
 *  has to defend against an inverted drag. */
data class SelectionRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun normalized(): SelectionRect =
        SelectionRect(
            left = minOf(left, right),
            top = minOf(top, bottom),
            right = maxOf(left, right),
            bottom = maxOf(top, bottom),
        )
}

data class DrawToolState(
    val activeTool: DrawToolType = DrawToolType.BRUSH,
    val brushSizePx: Float = 8f,
    val brushColorHex: String = "#FFD700",
    val brushOpacity: Float = 1f,
    val brushHardness: Float = 0.8f,
    val brushType: BrushType = BrushType.INK_PEN,
    val symmetryMode: SymmetryMode = SymmetryMode.OFF,
    // v0.4.30: touch-based simulated pressure — no stylus required.
    // When true, drawManualStroke derives a per-point width multiplier
    // from finger travel speed (slow = wide/heavy, fast = thin/light,
    // matching how real pressure-sensitive brushes feel) instead of a
    // flat stroke width, and applies a light moving-average smoothing
    // pass on raw touch points to remove finger jitter.
    val pressureSimulationEnabled: Boolean = true,
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
    // v0.4.30: selection tool state — see SelectionRect doc. Lives on the
    // project, not the tool state, so switching tools (e.g. to pick a
    // color) doesn't silently drop an active selection the user is
    // still working inside.
    val selection: SelectionRect? = null,
)
