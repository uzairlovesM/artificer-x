package com.waheed.artificerx.core.drawing.engine

import kotlin.math.max

class StrokeStabilizer {
    private var lastX = 0f
    private var lastY = 0f
    private var initialized = false

    fun reset() {
        initialized = false
        lastX = 0f
        lastY = 0f
    }

    fun apply(sample: StrokeSample, amount: Float, velocity: Float): StrokeSample {
        val strength = amount.coerceIn(0f, 1f)
        if (!initialized) {
            initialized = true
            lastX = sample.x
            lastY = sample.y
            return sample
        }
        val velocityFactor = 1f / (1f + velocity / 900f)
        val effective = (strength * velocityFactor).coerceIn(0f, 0.98f)
        val follow = max(0.02f, 1f - effective)
        lastX += (sample.x - lastX) * follow
        lastY += (sample.y - lastY) * follow
        return sample.copy(x = lastX, y = lastY)
    }
}
