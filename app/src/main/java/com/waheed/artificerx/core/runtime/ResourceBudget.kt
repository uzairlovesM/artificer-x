package com.waheed.artificerx.core.runtime

import kotlin.math.max
import kotlin.math.min

data class ResourceBudget(
    val memoryBytes: Long,
    val cpuUnits: Int,
    val ioUnits: Int,
    val networkUnits: Int
) {
    init {
        require(memoryBytes >= 0)
        require(cpuUnits >= 0 && ioUnits >= 0 && networkUnits >= 0)
    }

    fun clamp(other: ResourceBudget): ResourceBudget = ResourceBudget(
        memoryBytes = min(memoryBytes, other.memoryBytes),
        cpuUnits = min(cpuUnits, other.cpuUnits),
        ioUnits = min(ioUnits, other.ioUnits),
        networkUnits = min(networkUnits, other.networkUnits)
    )

    operator fun plus(other: ResourceBudget) = ResourceBudget(
        memoryBytes + other.memoryBytes,
        cpuUnits + other.cpuUnits,
        ioUnits + other.ioUnits,
        networkUnits + other.networkUnits
    )

    operator fun minus(other: ResourceBudget) = ResourceBudget(
        max(0, memoryBytes - other.memoryBytes),
        max(0, cpuUnits - other.cpuUnits),
        max(0, ioUnits - other.ioUnits),
        max(0, networkUnits - other.networkUnits)
    )

    fun canAfford(cost: ResourceBudget): Boolean =
        memoryBytes >= cost.memoryBytes &&
        cpuUnits >= cost.cpuUnits &&
        ioUnits >= cost.ioUnits &&
        networkUnits >= cost.networkUnits
}

class BudgetLedger(initial: ResourceBudget) {
    private var remaining = initial

    @Synchronized fun snapshot(): ResourceBudget = remaining

    @Synchronized
    fun reserve(cost: ResourceBudget): Boolean {
        if (!remaining.canAfford(cost)) return false
        remaining -= cost
        return true
    }

    @Synchronized fun release(cost: ResourceBudget) {
        remaining += cost
    }
}
