package com.waheed.artificerx.core.ai.runtime

/** Deterministic policy gate for AI-originated mutations. */
class CanvasOperationGuard {
    fun evaluate(proposal: CanvasOperationProposal, currentRevision: Long, hasSelection: Boolean): OperationDecision {
        if (proposal.expectedRevision != currentRevision) return OperationDecision(false, "stale_revision", 1f)
        if (proposal.operations.size > MAX_OPERATIONS) return OperationDecision(false, "operation_batch_too_large", 0.95f)
        val risk = proposal.operations.fold(0f) { total, op -> total + risk(op) } / proposal.operations.size
        if (risk >= 0.95f) return OperationDecision(false, "high_risk_operation", risk)
        if (proposal.operations.any { it is CanvasOperation.TranslateSelection } && !hasSelection) {
            return OperationDecision(false, "selection_required", 0.7f)
        }
        val normalized = proposal.copy(confidence = proposal.confidence.coerceIn(0f, 1f))
        return OperationDecision(true, if (proposal.requiresConfirmation) "confirmation_required" else "accepted", risk, normalized)
    }

    private fun risk(operation: CanvasOperation): Float = when (operation) {
        is CanvasOperation.RenameLayer -> 0.1f
        is CanvasOperation.SetLayerOpacity -> 0.15f
        is CanvasOperation.SetLayerVisibility -> 0.1f
        is CanvasOperation.SetLayerBlendMode -> 0.25f
        is CanvasOperation.SelectBounds -> 0.1f
        is CanvasOperation.TranslateSelection -> 0.35f
        is CanvasOperation.DrawStroke -> when {
            operation.points.size > 40_000 -> 0.9f
            operation.widthPx > 4_096f -> 0.75f
            else -> 0.3f
        }
    }

    companion object { private const val MAX_OPERATIONS = 128 }
}
