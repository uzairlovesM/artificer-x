package com.waheed.artificerx.drawing

import kotlin.math.hypot

class CanvasInteractionCoordinator {
    enum class GestureMode { NONE, DRAW, PAN, ZOOM, ROTATE, SELECTION }
    data class Point(val x: Float, val y: Float)
    data class Viewport(val scale: Float = 1f, val offsetX: Float = 0f, val offsetY: Float = 0f, val rotationDegrees: Float = 0f)

    private var mode = GestureMode.NONE
    private var viewport = Viewport()
    private var gestureStart = Point(0f, 0f)
    private var initialDistance = 0f
    private var initialScale = 1f

    fun begin(mode: GestureMode, primary: Point, secondary: Point? = null) {
        this.mode = mode
        gestureStart = primary
        initialDistance = secondary?.let { hypot(it.x - primary.x, it.y - primary.y) } ?: 0f
        initialScale = viewport.scale
    }

    fun update(primary: Point, secondary: Point? = null) {
        when (mode) {
            GestureMode.PAN -> {
                viewport = viewport.copy(offsetX = viewport.offsetX + primary.x - gestureStart.x, offsetY = viewport.offsetY + primary.y - gestureStart.y)
                gestureStart = primary
            }
            GestureMode.ZOOM -> {
                val distance = secondary?.let { hypot(it.x - primary.x, it.y - primary.y) } ?: return
                if (initialDistance > 0f) viewport = viewport.copy(scale = (initialScale * distance / initialDistance).coerceIn(0.05f, 64f))
            }
            GestureMode.ROTATE -> {
                val angle = kotlin.math.atan2(primary.y - gestureStart.y, primary.x - gestureStart.x) * 180f / Math.PI.toFloat()
                viewport = viewport.copy(rotationDegrees = viewport.rotationDegrees + angle.coerceIn(-30f, 30f))
            }
            else -> Unit
        }
    }

    fun end() { mode = GestureMode.NONE }
    fun viewport(): Viewport = viewport
    fun mapScreenToCanvas(point: Point): Point = Point((point.x - viewport.offsetX) / viewport.scale, (point.y - viewport.offsetY) / viewport.scale)
    fun mapCanvasToScreen(point: Point): Point = Point(point.x * viewport.scale + viewport.offsetX, point.y * viewport.scale + viewport.offsetY)
}
