package com.waheed.artificerx.domain

import com.waheed.artificerx.core.foundation.SubsystemHealth

class DomainRuntime {
    data class ProjectPolicy(val maxPixels: Long, val allowNetworkModels: Boolean, val requireExplicitDeletes: Boolean)
    data class ProjectDimensions(val width: Int, val height: Int)

    fun validate(dimensions: ProjectDimensions, policy: ProjectPolicy): List<String> {
        val errors = mutableListOf<String>()
        if (dimensions.width !in 1..32_768) errors += "width-out-of-range"
        if (dimensions.height !in 1..32_768) errors += "height-out-of-range"
        if (dimensions.width.toLong() * dimensions.height.toLong() > policy.maxPixels) errors += "pixel-budget-exceeded"
        return errors
    }

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "domain",
        readiness = 1.0,
        capabilities = setOf("project-policy", "dimension-validation", "pixel-budget"),
        invariants = listOf("bounded-canvas", "explicit-destructive-policy"),
        counters = emptyMap(),
    )
}
