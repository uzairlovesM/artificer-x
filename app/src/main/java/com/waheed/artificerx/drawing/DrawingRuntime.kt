package com.waheed.artificerx.drawing

import com.waheed.artificerx.core.foundation.SubsystemHealth

class DrawingRuntime {
    data class ToolState(val tool: String, val size: Int, val opacity: Float, val smoothing: Float)
    private var mutations = 0L

    fun normalize(state: ToolState): ToolState {
        mutations++
        return state.copy(size = state.size.coerceIn(1, 4096), opacity = state.opacity.coerceIn(0f, 1f), smoothing = state.smoothing.coerceIn(0f, 1f))
    }

    fun canDraw(enabled: Boolean, locked: Boolean, size: Int): Boolean = enabled && !locked && size in 1..4096

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "drawing",
        readiness = 1.0,
        capabilities = setOf("tool-state", "parameter-normalization", "lock-aware-authoring"),
        invariants = listOf("bounded-size", "bounded-opacity", "bounded-smoothing"),
        counters = mapOf("mutations" to mutations),
    )
}
