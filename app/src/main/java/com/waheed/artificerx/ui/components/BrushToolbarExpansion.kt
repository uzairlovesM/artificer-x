package com.waheed.artificerx.ui.components

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for BrushToolbar. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class BrushToolbarExpansion : ExpansionCapability {
    override val id: String = "ui.components.brushtoolbar"
    override val area: String = "ui.components"
    override val purpose: String = "BrushToolbar coordinates ui.components responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("BrushToolbar.input", "BrushToolbar.state", "BrushToolbar.output", "BrushToolbar.failure", "BrushToolbar.telemetry")

    override fun validate(): CapabilityCheck {
        val signalCount = contracts.count { it.isNotBlank() }
        return CapabilityCheck(
            id = id,
            ready = signalCount == contracts.size && contracts.isNotEmpty(),
            reason = if (signalCount == contracts.size) "contracts-ready" else "contract-gap",
            signals = mapOf(
                "contractCount" to signalCount.toString(),
                "area" to area,
                "purposeLength" to purpose.length.toString(),
            ),
        )
    }
}
