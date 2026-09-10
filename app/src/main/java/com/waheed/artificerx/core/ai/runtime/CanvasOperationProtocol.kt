package com.waheed.artificerx.core.ai.runtime

import java.util.UUID

/** AI proposes canvas mutations; it never writes directly to render state. */
data class CanvasOperationProposal(
    val id: String = UUID.randomUUID().toString(),
    val actor: String,
    val intent: String,
    val operations: List<CanvasOperation>,
    val expectedRevision: Long,
    val confidence: Float,
    val requiresConfirmation: Boolean = false,
    val rationale: String = "",
) {
    init {
        require(actor.isNotBlank() && intent.isNotBlank())
        require(operations.isNotEmpty())
        require(expectedRevision >= 0)
        require(confidence in 0f..1f)
    }
}

sealed interface CanvasOperation {
    data class RenameLayer(val layerId: String, val name: String) : CanvasOperation
    data class SetLayerOpacity(val layerId: String, val opacity: Float) : CanvasOperation
    data class SetLayerVisibility(val layerId: String, val visible: Boolean) : CanvasOperation
    data class SetLayerBlendMode(val layerId: String, val blendMode: String) : CanvasOperation
    data class SelectBounds(val left: Float, val top: Float, val right: Float, val bottom: Float) : CanvasOperation
    data class TranslateSelection(val dx: Float, val dy: Float) : CanvasOperation
    data class DrawStroke(
        val layerId: String,
        val points: List<Float>,
        val colorArgb: Int,
        val widthPx: Float,
        val opacity: Float,
        val brushId: String,
    ) : CanvasOperation {
        init {
            require(points.size >= 4 && points.size % 2 == 0)
            require(widthPx > 0f)
            require(opacity in 0f..1f)
        }
    }
}

data class OperationDecision(
    val accepted: Boolean,
    val reason: String,
    val riskScore: Float,
    val normalized: CanvasOperationProposal? = null,
)
