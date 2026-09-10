package com.waheed.artificerx.art

import com.waheed.artificerx.core.foundation.SubsystemHealth
import kotlin.math.hypot

class ArtRuntime {
    data class StrokeSample(val x: Float, val y: Float, val pressure: Float, val timeMs: Long)
    data class StrokeMetrics(val length: Float, val averagePressure: Float, val durationMs: Long, val samples: Int)

    private val samples = ArrayDeque<StrokeSample>()
    private var committedStrokes = 0L
    private var discardedStrokes = 0L

    fun begin(sample: StrokeSample) { samples.clear(); add(sample) }
    fun add(sample: StrokeSample) {
        require(sample.x.isFinite() && sample.y.isFinite())
        require(sample.pressure.isFinite() && sample.pressure in 0f..1f)
        if (samples.isEmpty() || sample.timeMs >= samples.last().timeMs) samples.add(sample)
    }
    fun end(minSamples: Int = 2): StrokeMetrics? {
        if (samples.size < minSamples) { discardedStrokes++; samples.clear(); return null }
        var length = 0f
        var pressure = 0f
        var previous = samples.first()
        samples.forEachIndexed { index, current ->
            pressure += current.pressure
            if (index > 0) length += hypot(current.x - previous.x, current.y - previous.y)
            previous = current
        }
        val result = StrokeMetrics(length, pressure / samples.size, (samples.last().timeMs - samples.first().timeMs).coerceAtLeast(0L), samples.size)
        committedStrokes++
        samples.clear()
        return result
    }

    fun cancel() { if (samples.isNotEmpty()) discardedStrokes++; samples.clear() }

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "art",
        readiness = if (discardedStrokes > committedStrokes + 8) 0.78 else 0.98,
        capabilities = setOf("stroke-metrics", "pressure-validation", "gesture-safe-authoring"),
        invariants = listOf("monotonic-time", "bounded-pressure", "minimum-sample-commit"),
        counters = mapOf("committedStrokes" to committedStrokes, "discardedStrokes" to discardedStrokes, "activeSamples" to samples.size.toLong()),
    )
}
