package com.waheed.artificerx.core.canvas

import kotlin.math.max
import kotlin.math.min

data class Viewport(val centerX: Float, val centerY: Float, val zoom: Float, val rotation: Float = 0f) {
    fun zoomBy(factor: Float): Viewport = copy(zoom = (zoom * factor).coerceIn(.02f, 64f))
    fun panBy(dx: Float, dy: Float): Viewport = copy(centerX = centerX + dx / zoom, centerY = centerY + dy / zoom)
    fun rotateBy(degrees: Float): Viewport = copy(rotation = ((rotation + degrees + 180f) % 360f) - 180f)
    fun clampTo(width: Float, height: Float): Viewport = copy(
        centerX = centerX.coerceIn(0f, max(0f, width)),
        centerY = centerY.coerceIn(0f, max(0f, height)),
        zoom = min(64f, max(.02f, zoom))
    )
}
