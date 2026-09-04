package com.waheed.artificerx.ui.screens.canvas

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for CanvasStudio. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class CanvasStudioExpansion : ExpansionCapability {
    override val id: String = "ui.screens.canvas.canvasstudio"
    override val area: String = "ui.screens.canvas"
    override val purpose: String = "CanvasStudio coordinates ui.screens.canvas responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("CanvasStudio.input", "CanvasStudio.state", "CanvasStudio.output", "CanvasStudio.failure", "CanvasStudio.telemetry")

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
