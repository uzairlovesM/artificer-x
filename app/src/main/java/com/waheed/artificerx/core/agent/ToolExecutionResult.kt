package com.waheed.artificerx.core.agent

sealed class ToolExecutionResult {
    data class Success(
        val message: String,
        val requiresSnapshot: Boolean = false,
    ) : ToolExecutionResult()

    data class Failure(
        val errorMessage: String,
    ) : ToolExecutionResult()

    data class TurnFinished(
        val summary: String,
    ) : ToolExecutionResult()
}

/**
 * Parsed, type-safe representation of a tool_call the LLM emitted,
 * after JSON-decoding the raw arguments string from FunctionCallDto.
 * ToolExecutor pattern-matches on this sealed hierarchy rather than
 * re-parsing JSON at every call site.
 */
sealed class ParsedToolCall {
    data class CreateLayer(
        val name: String,
    ) : ParsedToolCall()

    data class DeleteLayer(
        val layerId: String,
    ) : ParsedToolCall()

    data class SetActiveLayer(
        val layerId: String,
    ) : ParsedToolCall()

    data class DrawPath(
        val points: List<Float>,
        val colorHex: String?,
        val strokeWidthPx: Float?,
        val opacity: Float?,
        val brushType: com.waheed.artificerx.domain.model.BrushType?,
    ) : ParsedToolCall()

    data class DrawShape(
        val shapeType: String,
        val x: Float,
        val y: Float,
        val width: Float?,
        val height: Float?,
        val fillColorHex: String?,
        val strokeColorHex: String?,
        val strokeWidthPx: Float?,
        val rotationDegrees: Float?,
        val sides: Int?,
    ) : ParsedToolCall()

    data class ApplyGradient(
        val gradientType: String,
        val startColorHex: String,
        val endColorHex: String,
        val x: Float?,
        val y: Float?,
        val width: Float?,
        val height: Float?,
        val angleDegrees: Float?,
        val additionalColorStopsHex: List<String>?,
    ) : ParsedToolCall()

    data class FillRegion(
        val x: Float,
        val y: Float,
        val colorHex: String,
        val tolerance: Float?,
    ) : ParsedToolCall()

    data class SetLayerProperty(
        val layerId: String,
        val opacity: Float?,
        val blendMode: String?,
        val isVisible: Boolean?,
    ) : ParsedToolCall()

    data class DuplicateLayer(
        val sourceLayerId: String,
        val newName: String?,
    ) : ParsedToolCall()

    data class FlipLayer(
        val layerId: String,
        val horizontal: Boolean,
        val vertical: Boolean,
    ) : ParsedToolCall()

    data class CropCanvas(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int,
    ) : ParsedToolCall()

    object InspectCanvas : ParsedToolCall()

    data class PickColor(
        val x: Float,
        val y: Float,
    ) : ParsedToolCall()

    data class ApplyFilter(
        val layerId: String,
        val filterType: String,
        val intensity: Float?,
    ) : ParsedToolCall()

    data class AddText(
        val text: String,
        val x: Float,
        val y: Float,
        val fontSizePx: Float?,
        val colorHex: String?,
        val bold: Boolean?,
    ) : ParsedToolCall()

    data class CreateMask(
        val layerId: String,
        val maskShape: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val invert: Boolean?,
    ) : ParsedToolCall()

    data class EnableSymmetry(
        val mode: String,
    ) : ParsedToolCall()

    data class ApplyPattern(
        val patternType: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val colorHex: String,
        val scalePx: Float?,
    ) : ParsedToolCall()

    data class DrawCurve(
        val startX: Float,
        val startY: Float,
        val controlX: Float,
        val controlY: Float,
        val endX: Float,
        val endY: Float,
        val colorHex: String?,
        val strokeWidthPx: Float?,
    ) : ParsedToolCall()

    data class ImportImageLayer(
        val layerName: String,
        val opacity: Float?,
    ) : ParsedToolCall()

    // Section: Web search/fetch tools
    data class WebFetch(
        val url: String,
    ) : ParsedToolCall()

    data class WebSearch(
        val query: String,
    ) : ParsedToolCall()

    // v0.4.30: full project/canvas customization access for the AI —
    // previously the AI could only draw ON a canvas whose size,
    // background, and default brush settings a human had already fixed
    // in the UI. These let a turn like "make me a 2000x3000 poster with
    // a dark navy background" actually configure the project itself,
    // not just paint inside whatever was already there.
    data class ResizeCanvas(
        val widthPx: Int,
        val heightPx: Int,
    ) : ParsedToolCall()

    data class SetCanvasBackground(
        val colorHex: String,
    ) : ParsedToolCall()

    data class SetBrushDefaults(
        val brushType: com.waheed.artificerx.domain.model.BrushType?,
        val sizePx: Float?,
        val colorHex: String?,
        val opacity: Float?,
        val hardness: Float?,
    ) : ParsedToolCall()

    // v0.4.30: AI-callable selection & transform — previously these
    // existed only for manual touch input (CanvasTouchOverlow) despite
    // the system prompt claiming the AI could use them, which was a
    // real bug (the model would call a tool that didn't exist and get
    // an "Unknown tool" failure). Backed by the same StudioViewModel
    // methods the manual UI uses, so an AI-driven selection/transform
    // and a human-driven one behave identically.
    data class SetSelection(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) : ParsedToolCall()

    object ClearSelection : ParsedToolCall()

    object DeleteSelectionContent : ParsedToolCall()

    data class TransformLayer(
        val dx: Float,
        val dy: Float,
        val scaleFactor: Float,
        val rotationDegrees: Float,
        val pivotX: Float?,
        val pivotY: Float?,
    ) : ParsedToolCall()

    // 3D sculpting tool calls
    data class CreatePrimitive(
        val primitiveType: String,
        val name: String,
    ) : ParsedToolCall()

    data class SculptStroke(
        val meshId: String,
        val brushType: String,
        val hitX: Float,
        val hitY: Float,
        val hitZ: Float,
        val radius: Float?,
        val strength: Float?,
    ) : ParsedToolCall()

    data class DeleteMesh(
        val meshId: String,
    ) : ParsedToolCall()

    data class SetMeshColor(
        val meshId: String,
        val colorHex: String,
    ) : ParsedToolCall()

    data class TransformMesh(
        val meshId: String,
        val positionX: Float?,
        val positionY: Float?,
        val positionZ: Float?,
        val rotationXDegrees: Float?,
        val rotationYDegrees: Float?,
        val rotationZDegrees: Float?,
        val scaleX: Float?,
        val scaleY: Float?,
        val scaleZ: Float?,
    ) : ParsedToolCall()

    object InspectScene : ParsedToolCall()

    data class FinishTurn(
        val summary: String,
    ) : ParsedToolCall()

    data class Unknown(
        val toolName: String,
    ) : ParsedToolCall()
}
