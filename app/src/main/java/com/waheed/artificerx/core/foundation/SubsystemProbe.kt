package com.waheed.artificerx.core.foundation

import kotlin.math.roundToInt

data class SubsystemHealth(
    val id: String,
    val readiness: Double,
    val capabilities: Set<String>,
    val invariants: List<String>,
    val counters: Map<String, Long>,
    val warnings: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank())
        require(readiness in 0.0..1.0)
        require(capabilities.none { it.isBlank() })
    }

    val percentage: Int get() = (readiness * 100.0).roundToInt().coerceIn(0, 100)
}

fun interface SubsystemProbe {
    fun inspect(): SubsystemHealth
}
