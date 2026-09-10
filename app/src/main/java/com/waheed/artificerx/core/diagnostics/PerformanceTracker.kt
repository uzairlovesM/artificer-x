package com.waheed.artificerx.core.diagnostics

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class PerformanceSample(val count: Long, val totalMillis: Long, val maxMillis: Long) {
    val averageMillis: Double get() = if (count == 0L) 0.0 else totalMillis.toDouble() / count
}

class PerformanceTracker {
    private data class MutableSample(val count: AtomicLong = AtomicLong(), val total: AtomicLong = AtomicLong(), val max: AtomicLong = AtomicLong())
    private val samples = ConcurrentHashMap<String, MutableSample>()

    fun record(name: String, elapsedMillis: Long) {
        val sample = samples.computeIfAbsent(name) { MutableSample() }
        sample.count.incrementAndGet()
        sample.total.addAndGet(elapsedMillis.coerceAtLeast(0L))
        sample.max.updateAndGet { maxOf(it, elapsedMillis.coerceAtLeast(0L)) }
    }

    fun snapshot(): Map<String, PerformanceSample> = samples.mapValues {
        PerformanceSample(it.value.count.get(), it.value.total.get(), it.value.max.get())
    }
}
