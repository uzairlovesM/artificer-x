package com.waheed.artificerx.ui

import com.waheed.artificerx.core.foundation.SubsystemHealth

class UiRuntime {
    data class PanelState(val id: String, val visible: Boolean, val widthDp: Int, val order: Int)
    fun normalize(panels: List<PanelState>): List<PanelState> = panels
        .distinctBy { it.id }
        .map { it.copy(widthDp = it.widthDp.coerceIn(0, 720), order = it.order.coerceAtLeast(0)) }
        .sortedWith(compareBy<PanelState> { it.order }.thenBy { it.id })

    fun visibleWidth(panels: List<PanelState>): Int = normalize(panels).filter { it.visible }.sumOf { it.widthDp }

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "ui",
        readiness = 1.0,
        capabilities = setOf("panel-normalization", "stable-ordering", "layout-bounds"),
        invariants = listOf("unique-panel-ids", "bounded-panel-width", "stable-order"),
        counters = emptyMap(),
    )
}
