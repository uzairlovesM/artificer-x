package com.waheed.artificerx.core.canvas

import kotlin.math.cos
import kotlin.math.sin

/** Stable canvas/screen mapping with zoom, rotation and arbitrary screen origin. */
class CanvasCoordinateMapper {
    data class Point(val x: Float, val y: Float)
    data class Transform(val originX: Float, val originY: Float, val zoom: Float, val rotationDegrees: Float)

    fun canvasToScreen(point: Point, t: Transform): Point {
        val local = rotate(point.x, point.y, t.rotationDegrees)
        return Point(t.originX + local.x * t.zoom, t.originY + local.y * t.zoom)
    }

    fun screenToCanvas(point: Point, t: Transform): Point {
        require(t.zoom > 0f)
        val localX = (point.x - t.originX) / t.zoom
        val localY = (point.y - t.originY) / t.zoom
        return rotate(localX, localY, -t.rotationDegrees)
    }

    fun zoomAroundScreenPoint(current: Transform, factor: Float, anchor: Point): Transform {
        require(factor > 0f)
        val before = screenToCanvas(anchor, current)
        val next = current.copy(zoom = (current.zoom * factor).coerceIn(0.02f, 64f))
        val rotated = rotate(before.x, before.y, next.rotationDegrees)
        return next.copy(
            originX = anchor.x - rotated.x * next.zoom,
            originY = anchor.y - rotated.y * next.zoom,
        )
    }

    private fun rotate(x: Float, y: Float, degrees: Float): Point {
        val radians = Math.toRadians(degrees.toDouble())
        val c = cos(radians).toFloat(); val s = sin(radians).toFloat()
        return Point(x * c - y * s, x * s + y * c)
    }
}
