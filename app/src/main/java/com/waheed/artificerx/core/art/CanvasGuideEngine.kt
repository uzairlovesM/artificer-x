package com.waheed.artificerx.core.art

import kotlin.math.max
import kotlin.math.min

/** Pure geometry for editor guides, grids and symmetry overlays. */
class CanvasGuideEngine {
    data class Line(val x1: Float, val y1: Float, val x2: Float, val y2: Float)

    fun grid(width: Int, height: Int, subdivisions: Int = 8): List<Line> {
        val n = subdivisions.coerceIn(2, 32)
        val lines = ArrayList<Line>(n * 2 + 2)
        for (i in 1 until n) {
            val x = width * i / n.toFloat()
            val y = height * i / n.toFloat()
            lines += Line(x, 0f, x, height.toFloat())
            lines += Line(0f, y, width.toFloat(), y)
        }
        return lines
    }

    fun safeArea(width: Int, height: Int, insetFraction: Float = 0.08f): List<Line> {
        val ix = max(1f, width * insetFraction.coerceIn(0.01f, 0.25f))
        val iy = max(1f, height * insetFraction.coerceIn(0.01f, 0.25f))
        val l = min(width.toFloat() - 1f, ix)
        val t = min(height.toFloat() - 1f, iy)
        val r = max(l + 1f, width - ix)
        val b = max(t + 1f, height - iy)
        return listOf(Line(l, t, r, t), Line(r, t, r, b), Line(r, b, l, b), Line(l, b, l, t))
    }
}
