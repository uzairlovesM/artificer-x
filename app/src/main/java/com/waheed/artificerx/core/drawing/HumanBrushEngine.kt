package com.waheed.artificerx.core.drawing

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Deterministic pressure, velocity, smoothing and taper dynamics for drawing. */
object HumanBrushEngine {
    fun applyDynamics(
        points: List<Float>,
        baseWeights: List<Float>?,
        sizePressure: Float,
        opacityPressure: Float,
        smoothing: Float,
        taperStart: Float,
        taperEnd: Float,
        wetness: Float = 0f,
        bleed: Float = 0f,
    ): List<Float>? {
        if (points.size < 4) return baseWeights
        val count = points.size / 2
        val weights = MutableList(count) { i -> (baseWeights?.getOrNull(i) ?: 1f).coerceIn(0.08f, 3f) }
        val speeds = FloatArray(count)
        for (i in 1 until count) {
            val dx = points[i * 2] - points[(i - 1) * 2]
            val dy = points[i * 2 + 1] - points[(i - 1) * 2 + 1]
            speeds[i] = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        }
        val maxSpeed = speeds.drop(1).maxOrNull()?.coerceAtLeast(1f) ?: 1f
        var totalDistance = 0f
        for (i in 1 until count) totalDistance += speeds[i]
        var travelled = 0f
        for (i in 0 until count) {
            if (i > 0) travelled += speeds[i]
            val speed01 = (speeds[i] / maxSpeed).coerceIn(0f, 1f)
            val dynamicPressure = 1.12f - speed01 * 0.42f
            val pressureBlend = 0.45f + sizePressure * 0.55f
            val opacityBlend = 0.35f + opacityPressure * 0.65f
            val taperIn = if (taperStart > 0f && totalDistance > 0f) min(1f, travelled / max(1f, totalDistance * taperStart)) else 1f
            val taperOut = if (taperEnd > 0f && totalDistance > 0f) min(1f, (totalDistance - travelled) / max(1f, totalDistance * taperEnd)) else 1f
            val taper = min(taperIn, taperOut).coerceIn(0f, 1f)
            val wetLift = 1f + wetness.coerceIn(0f, 1f) * (0.18f + (1f - speed01) * 0.12f)
            val bleedSoftening = 1f + bleed.coerceIn(0f, 1f) * 0.16f
            weights[i] = (weights[i] * (dynamicPressure * pressureBlend + (1f - opacityBlend) * 0.08f) * wetLift * bleedSoftening * (0.55f + taper * 0.45f)).coerceIn(0.08f, 2.8f)
        }
        val passCount = (1 + (smoothing * 3f).toInt()).coerceIn(1, 4)
        repeat(passCount - 1) {
            for (i in 1 until count - 1) weights[i] = (weights[i - 1] + weights[i] * 2f + weights[i + 1]) / 4f
        }
        return weights
    }
    /** Applies deterministic micro-variation to a stroke path. The variation is
     * stable for the same point sequence, so undo/redo and AI replays do not
     * produce different pixels while still breaking the mathematically-perfect
     * vector look of pencil, charcoal and dry brushes. */
    fun applySpatialScatter(
        points: List<Float>,
        scatter: Float,
        brushSizePx: Float,
    ): List<Float> {
        if (scatter <= 0f || points.size < 4) return points
        val amount = (brushSizePx * scatter.coerceIn(0f, 1f) * 0.16f).coerceAtMost(14f)
        if (amount <= 0.05f) return points
        val result = points.toMutableList()
        var point = 0
        while (point * 2 + 1 < points.size) {
            val phase = point * 1.6180339f
            val dx = kotlin.math.sin(phase * 2.41f) * amount
            val dy = kotlin.math.cos(phase * 1.73f) * amount
            result[point * 2] = points[point * 2] + dx
            result[point * 2 + 1] = points[point * 2 + 1] + dy
            point += 1
        }
        return result
    }

}
