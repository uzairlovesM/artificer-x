package com.waheed.artificerx.core.drawing

import kotlin.math.max
import kotlin.math.min

data class BrushDynamics(
    val size: Float,
    val opacity: Float,
    val spacing: Float,
    val pressureCurve: PressureCurve = PressureCurve.Linear
) {
    init {
        require(size > 0f)
        require(opacity in 0f..1f)
        require(spacing > 0f)
    }

    fun sample(point: StrokePoint): Sample {
        val pressure = pressureCurve.map(point.pressure)
        val tiltFactor = 1f - min(1f, kotlin.math.abs(point.tilt) / 90f) * .35f
        return Sample(
            radius = max(.25f, size * (.15f + .85f * pressure) * tiltFactor),
            opacity = min(1f, opacity * (.2f + .8f * pressure))
        )
    }

    data class Sample(val radius: Float, val opacity: Float)
}
