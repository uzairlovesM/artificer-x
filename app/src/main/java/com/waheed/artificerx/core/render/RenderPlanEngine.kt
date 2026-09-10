package com.waheed.artificerx.core.render

import com.waheed.artificerx.core.canvas.RectFInt
import com.waheed.artificerx.core.canvas.Tile
import com.waheed.artificerx.core.canvas.TilePlanner
import kotlin.math.max
import kotlin.math.min

/** Builds deterministic incremental render work instead of forcing full-canvas redraws. */
class RenderPlanEngine(private val tilePlanner: TilePlanner = TilePlanner(256)) {
    data class RenderPlan(
        val bounds: RectFInt,
        val tiles: List<Tile>,
        val fullFrame: Boolean,
        val estimatedPixels: Long,
        val reason: String,
    )

    fun plan(canvasWidth: Int, canvasHeight: Int, dirty: RectFInt?, forceFull: Boolean = false, reason: String = "mutation"): RenderPlan {
        require(canvasWidth > 0 && canvasHeight > 0)
        val bounds = dirty?.let { clamp(it, canvasWidth, canvasHeight) }
            ?: RectFInt(0, 0, canvasWidth, canvasHeight)
        val full = forceFull || dirty == null || bounds.area >= canvasWidth.toLong() * canvasHeight * 0.80
        val finalBounds = if (full) RectFInt(0, 0, canvasWidth, canvasHeight) else bounds
        val tiles = tilePlanner.visibleTiles(finalBounds.left.toFloat(), finalBounds.top.toFloat(), finalBounds.right.toFloat(), finalBounds.bottom.toFloat())
        return RenderPlan(finalBounds, tiles, full, finalBounds.area, reason)
    }

    private fun clamp(rect: RectFInt, width: Int, height: Int): RectFInt = RectFInt(
        max(0, min(width, rect.left)),
        max(0, min(height, rect.top)),
        max(0, min(width, rect.right)),
        max(0, min(height, rect.bottom)),
    )
}
