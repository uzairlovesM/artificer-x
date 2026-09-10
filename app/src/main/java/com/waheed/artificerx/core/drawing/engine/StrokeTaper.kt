package com.waheed.artificerx.core.drawing.engine

import kotlin.math.pow

data class StrokeTaperConfig(
    val startLength: Float = 0f,
    val endLength: Float = 0f,
    val startExponent: Float = 1f,
    val endExponent: Float = 1f,
)

object StrokeTaper {
    fun multiplier(normalizedPosition: Float, config: StrokeTaperConfig): Float {
        val p = normalizedPosition.coerceIn(0f, 1f)
        val start = if (config.startLength <= 0f) 1f else (p / config.startLength.coerceAtLeast(0.0001f)).coerceIn(0f, 1f).pow(config.startExponent.coerceAtLeast(0.1f))
        val end = if (config.endLength <= 0f) 1f else ((1f - p) / config.endLength.coerceAtLeast(0.0001f)).coerceIn(0f, 1f).pow(config.endExponent.coerceAtLeast(0.1f))
        return (start * end).coerceIn(0.01f, 1f)
    }
}
