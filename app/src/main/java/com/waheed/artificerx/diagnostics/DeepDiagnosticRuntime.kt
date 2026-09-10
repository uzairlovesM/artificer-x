package com.waheed.artificerx.diagnostics

import com.waheed.artificerx.core.foundation.SubsystemHealth
import kotlin.math.max

class DeepDiagnosticRuntime {
    data class Metric(val name: String, val value: Double, val threshold: Double, val direction: Direction)
    enum class Direction { BELOW_IS_BETTER, ABOVE_IS_BETTER }

    fun failing(metrics: List<Metric>): List<Metric> = metrics.filter {
        when (it.direction) {
            Direction.BELOW_IS_BETTER -> it.value > it.threshold
            Direction.ABOVE_IS_BETTER -> it.value < it.threshold
        }
    }

    fun score(metrics: List<Metric>): Double {
        if (metrics.isEmpty()) return 1.0
        val pass = metrics.size - failing(metrics).size
        return max(0.0, pass.toDouble() / metrics.size)
    }

    fun inspect(metrics: List<Metric> = emptyList()): SubsystemHealth = SubsystemHealth(
        id = "diagnostics",
        readiness = score(metrics),
        capabilities = setOf("threshold-evaluation", "health-scoring", "metric-failure-reporting"),
        invariants = listOf("deterministic-health-score"),
        counters = mapOf("metrics" to metrics.size.toLong(), "failures" to failing(metrics).size.toLong()),
    )
}
