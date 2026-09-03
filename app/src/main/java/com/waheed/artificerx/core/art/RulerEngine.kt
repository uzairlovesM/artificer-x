package com.waheed.artificerx.core.art

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

class RulerEngine {
    enum class Mode { FREE, LINE, CIRCLE, RADIAL, PERSPECTIVE }
    fun snap(points: List<Float>, mode: Mode, anchorX: Float, anchorY: Float, radialSteps: Int = 8): List<Float> {
        if (points.size < 4 || mode == Mode.FREE) return points
        val out = points.toMutableList()
        var i = 0
        while (i + 1 < out.size) {
            val x = out[i]; val y = out[i + 1]
            when (mode) {
                Mode.LINE -> {
                    val dx = x - anchorX; val dy = y - anchorY
                    if (kotlin.math.abs(dx) >= kotlin.math.abs(dy)) out[i + 1] = anchorY else out[i] = anchorX
                }
                Mode.CIRCLE -> {
                    val r = kotlin.math.hypot(x - anchorX, y - anchorY).coerceAtLeast(1f)
                    val angle = atan2(y - anchorY, x - anchorX)
                    out[i] = anchorX + r * cos(angle); out[i + 1] = anchorY + r * sin(angle)
                }
                Mode.RADIAL -> {
                    val r = kotlin.math.hypot(x - anchorX, y - anchorY).coerceAtLeast(1f)
                    val step = (2.0 * PI / radialSteps.coerceAtLeast(2)).toFloat()
                    val angle = atan2(y - anchorY, x - anchorX)
                    val snapped = kotlin.math.round(angle / step) * step
                    out[i] = anchorX + r * cos(snapped); out[i + 1] = anchorY + r * sin(snapped)
                }
                Mode.PERSPECTIVE -> {
                    val scale = ((y - anchorY) / 1000f).coerceIn(-1f, 1f)
                    out[i] = anchorX + (x - anchorX) * (1f + scale * 0.2f)
                }
                Mode.FREE -> Unit
            }
            i += 2
        }
        return out
    }
}
