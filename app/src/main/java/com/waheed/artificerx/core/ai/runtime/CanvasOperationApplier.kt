package com.waheed.artificerx.core.ai.runtime

import com.waheed.artificerx.core.canvas.CanvasState
import com.waheed.artificerx.core.canvas.SelectionState
import com.waheed.artificerx.core.project.LayerId
import com.waheed.artificerx.core.project.ProjectGraph

/** Applies safe structural AI operations and returns pixel operations for the renderer to execute. */
class CanvasOperationApplier {
    data class Result(
        val appliedStructuralOperations: Int,
        val deferredPixelOperations: List<CanvasOperation>,
        val canvasState: CanvasState,
    )

    fun apply(proposal: CanvasOperationProposal, project: ProjectGraph, canvas: CanvasState): Result {
        var nextCanvas = canvas
        var structural = 0
        val pixels = mutableListOf<CanvasOperation>()
        proposal.operations.forEach { operation ->
            when (operation) {
                is CanvasOperation.RenameLayer -> { project.updateLayer(LayerId(operation.layerId)) { it.copy(name = operation.name.trim()) }; structural++ }
                is CanvasOperation.SetLayerOpacity -> { project.updateLayer(LayerId(operation.layerId)) { it.copy(opacity = operation.opacity.coerceIn(0f, 1f)) }; structural++ }
                is CanvasOperation.SetLayerVisibility -> { project.updateLayer(LayerId(operation.layerId)) { it.copy(visible = operation.visible) }; structural++ }
                is CanvasOperation.SetLayerBlendMode -> { project.updateLayer(LayerId(operation.layerId)) { it.copy(blendMode = operation.blendMode.lowercase().trim()) }; structural++ }
                is CanvasOperation.SelectBounds -> { nextCanvas = nextCanvas.copy(selection = SelectionState.Bounds(operation.left, operation.top, operation.right, operation.bottom), dirty = true); structural++ }
                is CanvasOperation.TranslateSelection -> { require(nextCanvas.selection !is SelectionState.Empty); nextCanvas = nextCanvas.markDirty(); pixels += operation }
                is CanvasOperation.DrawStroke -> { pixels += operation }
            }
        }
        return Result(structural, pixels, nextCanvas)
    }
}
