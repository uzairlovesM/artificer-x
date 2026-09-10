package com.waheed.artificerx.core.drawing.engine

import kotlin.math.max

data class ViewportTransform(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1f,
) {
    private val fitScale: Float get() = minOf(viewportWidth / max(canvasWidth, 1f), viewportHeight / max(canvasHeight, 1f))
    val scale: Float get() = fitScale * zoom.coerceIn(.05f, 32f)
    val offsetX: Float get() = (viewportWidth - canvasWidth * scale) * .5f + panX
    val offsetY: Float get() = (viewportHeight - canvasHeight * scale) * .5f + panY

    fun screenToCanvas(x: Float, y: Float): Pair<Float, Float> = ((x - offsetX) / scale) to ((y - offsetY) / scale)
    fun canvasToScreen(x: Float, y: Float): Pair<Float, Float> = (x * scale + offsetX) to (y * scale + offsetY)

    fun zoomAround(screenX: Float, screenY: Float, factor: Float): ViewportTransform {
        val oldScale = scale
        val nextZoom = (zoom * factor).coerceIn(.05f, 32f)
        val nextScale = fitScale * nextZoom
        val canvasX = (screenX - offsetX) / oldScale
        val canvasY = (screenY - offsetY) / oldScale
        val nextOffsetX = screenX - canvasX * nextScale
        val nextOffsetY = screenY - canvasY * nextScale
        val centeredX = (viewportWidth - canvasWidth * nextScale) * .5f
        val centeredY = (viewportHeight - canvasHeight * nextScale) * .5f
        return copy(zoom = nextZoom, panX = nextOffsetX - centeredX, panY = nextOffsetY - centeredY)
    }
}
