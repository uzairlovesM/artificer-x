package com.waheed.artificerx.core.drawing.engine

import kotlin.math.hypot

class StrokeVelocityEstimator(private val smoothing: Float = 0.35f) {
    private var previous: StrokeSample? = null
    private var filtered = 0f

    fun reset() {
        previous = null
        filtered = 0f
    }

    fun velocity(sample: StrokeSample): Float {
        val before = previous
        previous = sample
        if (before == null) return filtered
        val distance = hypot(sample.x - before.x, sample.y - before.y)
        val dt = (sample.timestampNanos - before.timestampNanos).coerceAtLeast(1L) / 1_000_000_000f
        val instant = if (before.timestampNanos == 0L || sample.timestampNanos == 0L) distance else distance / dt
        filtered += (instant - filtered) * smoothing.coerceIn(0.01f, 1f)
        return filtered
    }
}
