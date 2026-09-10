package com.waheed.artificerx.core.drawing.engine

import com.waheed.artificerx.core.drawing.StrokeSimplifier
import kotlin.math.hypot

/**
 * Provider-independent drawing pipeline shared by touch input, stylus input,
 * replay and AI-generated strokes. It keeps geometry processing deterministic
 * so the same command produces the same raster input.
 */
class StrokePipeline(
    private val velocityEstimator: StrokeVelocityEstimator = StrokeVelocityEstimator(),
    private val stabilizer: StrokeStabilizer = StrokeStabilizer(),
    private val dynamics: StrokeDynamicsEngine = StrokeDynamicsEngine(),
) {
    data class Config(
        val smoothing: Float,
        val spacing: Float,
        val pressureResponse: Float,
        val dynamics: StrokeDynamicsConfig = StrokeDynamicsConfig(),
        val taper: StrokeTaperConfig = StrokeTaperConfig(),
        val simplifyTolerance: Float = 0.35f,
    )

    data class Result(
        val points: List<Float>,
        val segmentWeights: List<Float>,
        val velocities: List<Float>,
        val quality: Float,
        val originalPointCount: Int,
        val outputPointCount: Int,
    )

    fun process(points: List<Float>, pressureWeights: List<Float>?, config: Config): Result {
        if (points.size < 4 || points.size % 2 != 0) {
            return Result(points, emptyList(), emptyList(), 0f, points.size / 2, points.size / 2)
        }
        velocityEstimator.reset()
        stabilizer.reset()
        val input = points.toStrokeSamples(pressureWeights)
        val stabilized = ArrayList<StrokeSample>(input.size)
        var pressure = input.first().pressure
        input.forEach { sample ->
            pressure = dynamics.stabilizePressure(pressure, sample.pressure, config.pressureResponse)
            val velocity = velocityEstimator.velocity(sample)
            stabilized += stabilizer.apply(sample.copy(pressure = pressure), config.smoothing, velocity)
        }
        if (stabilized.isNotEmpty()) {
            stabilized[0] = stabilized.first().copy(x = input.first().x, y = input.first().y)
            if (stabilized.size > 1) {
                stabilized[stabilized.lastIndex] = stabilized.last().copy(x = input.last().x, y = input.last().y)
            }
        }
        val simplified = StrokeSimplifier.simplify(
            stabilized.map { com.waheed.artificerx.core.drawing.StrokePoint(it.x, it.y, it.pressure, it.tilt, it.timestampNanos) },
            config.simplifyTolerance * (1f - config.smoothing * 0.55f),
        ).map { StrokeSample(it.x, it.y, it.pressure, it.timestamp) }
        val spaced = resample(simplified, config.spacing)
        val weights = ArrayList<Float>(spaced.size - 1)
        val velocities = ArrayList<Float>(spaced.size)
        var totalLength = 0f
        for (i in 1 until spaced.size) totalLength += hypot(spaced[i].x - spaced[i - 1].x, spaced[i].y - spaced[i - 1].y)
        var travelled = 0f
        for (i in spaced.indices) {
            if (i > 0) travelled += hypot(spaced[i].x - spaced[i - 1].x, spaced[i].y - spaced[i - 1].y)
            val position = if (totalLength <= 0.001f) 1f else travelled / totalLength
            val velocity = if (i == 0) 0f else hypot(spaced[i].x - spaced[i - 1].x, spaced[i].y - spaced[i - 1].y)
            velocities += velocity
            if (i > 0) {
                val dynamic = dynamics.calculate(spaced[i].pressure, velocity, spaced[i].tilt, config.dynamics)
                weights += dynamic.widthMultiplier * StrokeTaper.multiplier(position, config.taper)
            }
        }
        val quality = quality(input, spaced)
        return Result(spaced.toFloatPoints(), weights, velocities, quality, input.size, spaced.size)
    }

    private fun resample(input: List<StrokeSample>, spacing: Float): List<StrokeSample> {
        if (input.size < 2) return input
        val target = (0.65f + spacing.coerceIn(0.01f, 1f) * 8f).coerceAtLeast(0.65f)
        val output = ArrayList<StrokeSample>(input.size)
        output += input.first()
        var previous = input.first()
        var carry = 0f
        for (current in input.drop(1)) {
            var dx = current.x - previous.x
            var dy = current.y - previous.y
            var distance = hypot(dx, dy)
            if (distance <= 0.0001f) continue
            while (carry + distance >= target) {
                val ratio = ((target - carry) / distance).coerceIn(0f, 1f)
                val next = StrokeSample(
                    x = previous.x + dx * ratio,
                    y = previous.y + dy * ratio,
                    pressure = previous.pressure + (current.pressure - previous.pressure) * ratio,
                    timestampNanos = previous.timestampNanos + ((current.timestampNanos - previous.timestampNanos) * ratio).toLong(),
                    tilt = previous.tilt + (current.tilt - previous.tilt) * ratio,
                    azimuth = previous.azimuth + (current.azimuth - previous.azimuth) * ratio,
                )
                output += next
                previous = next
                carry = 0f
                dx = current.x - previous.x
                dy = current.y - previous.y
                distance = hypot(dx, dy)
                if (distance <= 0.0001f) break
            }
            carry += distance
            previous = current
        }
        if (output.last().x != input.last().x || output.last().y != input.last().y) output += input.last()
        return output
    }

    private fun quality(before: List<StrokeSample>, after: List<StrokeSample>): Float {
        if (before.size < 2 || after.size < 2) return 0f
        val reduction = (1f - after.size.toFloat() / before.size.toFloat()).coerceIn(0f, 1f)
        var direction = 0f
        var count = 0
        for (i in 2 until after.size) {
            val ax = after[i - 1].x - after[i - 2].x
            val ay = after[i - 1].y - after[i - 2].y
            val bx = after[i].x - after[i - 1].x
            val by = after[i].y - after[i - 1].y
            val a = hypot(ax, ay)
            val b = hypot(bx, by)
            if (a > 0.01f && b > 0.01f) {
                direction += ((ax * bx + ay * by) / (a * b)).coerceIn(-1f, 1f) * 0.5f + 0.5f
                count++
            }
        }
        val continuity = if (count == 0) 0.5f else direction / count
        return (continuity * 0.78f + reduction * 0.22f).coerceIn(0f, 1f)
    }
}
