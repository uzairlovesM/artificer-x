package com.waheed.artificerx.core.canvas

import kotlin.math.abs

/** Authoritative canvas interaction state. Renderers consume this state; UI never mutates pixels directly. */
data class CanvasState(
    val widthPx: Int,
    val heightPx: Int,
    val viewport: Viewport,
    val activeLayerId: String?,
    val tool: CanvasTool = CanvasTool.BRUSH,
    val selection: SelectionState = SelectionState.Empty,
    val pendingStrokePoints: List<Float> = emptyList(),
    val interactionSequence: Long = 0L,
    val dirty: Boolean = false,
) {
    init {
        require(widthPx > 0 && heightPx > 0)
        require(interactionSequence >= 0)
    }

    fun withViewport(next: Viewport): CanvasState = copy(viewport = next, interactionSequence = interactionSequence + 1)
    fun markDirty(): CanvasState = copy(dirty = true, interactionSequence = interactionSequence + 1)
    fun clean(): CanvasState = copy(dirty = false)
}

enum class CanvasTool { BRUSH, ERASER, SMUDGE, PICKER, FILL, LASSO, RECT_SELECT, ELLIPSE_SELECT, MOVE, TRANSFORM, SHAPE }

sealed interface SelectionState {
    data object Empty : SelectionState
    data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) : SelectionState {
        init { require(right >= left && bottom >= top) }
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
        fun area(): Float = width * height
        fun expanded(px: Float): Bounds = Bounds(left - px, top - px, right + px, bottom + px)
    }

    data class Path(val points: List<Float>) : SelectionState {
        init { require(points.size >= 6 && points.size % 2 == 0) }
        fun bounds(): Bounds {
            var left = Float.POSITIVE_INFINITY
            var top = Float.POSITIVE_INFINITY
            var right = Float.NEGATIVE_INFINITY
            var bottom = Float.NEGATIVE_INFINITY
            var i = 0
            while (i + 1 < points.size) {
                val x = points[i]; val y = points[i + 1]
                left = minOf(left, x); top = minOf(top, y); right = maxOf(right, x); bottom = maxOf(bottom, y)
                i += 2
            }
            return Bounds(left, top, right, bottom)
        }
    }

    fun areaOrZero(): Float = when (this) {
        Empty -> 0f
        is Bounds -> area()
        is Path -> bounds().area()
    }
}

object CanvasMath {
    fun distance(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax; val dy = by - ay
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun nearlyEquals(a: Float, b: Float, epsilon: Float = 0.0001f): Boolean = abs(a - b) <= epsilon
}
