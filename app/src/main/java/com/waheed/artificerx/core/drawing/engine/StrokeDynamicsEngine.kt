package com.waheed.artificerx.core.drawing.engine

import kotlin.math.abs
import kotlin.math.sqrt

data class StrokeDynamicsConfig(
    val sizePressure: Float = 0.75f,
    val opacityPressure: Float = 0.35f,
    val speedSize: Float = 0.18f,
    val speedOpacity: Float = 0.08f,
    val tiltSize: Float = 0.12f,
    val minMultiplier: Float = 0.12f,
    val maxMultiplier: Float = 1.8f,
)

data class DynamicStrokeValue(
    val widthMultiplier: Float,
    val opacityMultiplier: Float,
    val velocity: Float,
)

class StrokeDynamicsEngine {
    fun calculate(
        pressure: Float,
        velocity: Float,
        tilt: Float,
        config: StrokeDynamicsConfig,
    ): DynamicStrokeValue {
        val p = pressure.coerceIn(0f, 1f)
        val speed = (velocity / 2600f).coerceIn(0f, 1f)
        val tiltFactor = (abs(tilt) / 90f).coerceIn(0f, 1f)
        val pressureTerm = 1f + (p - 0.5f) * config.sizePressure * 1.5f
        val speedTerm = 1f - speed * config.speedSize
        val tiltTerm = 1f + tiltFactor * config.tiltSize
        val width = (pressureTerm * speedTerm * tiltTerm).coerceIn(config.minMultiplier, config.maxMultiplier)
        val opacity = (1f + (p - 0.5f) * config.opacityPressure - speed * config.speedOpacity)
            .coerceIn(0.05f, 1.35f)
        return DynamicStrokeValue(width, opacity, velocity)
    }

    fun stabilizePressure(previous: Float, current: Float, response: Float): Float {
        val alpha = (0.08f + response.coerceIn(0f, 1f) * 0.72f)
        return previous + (current.coerceIn(0f, 1f) - previous) * alpha
    }
}
