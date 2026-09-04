package com.waheed.artificerx.ui.screens.canvas

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for CanvasManga. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class CanvasMangaExpansion : ExpansionCapability {
    override val id: String = "ui.screens.canvas.canvasmanga"
    override val area: String = "ui.screens.canvas"
    override val purpose: String = "CanvasManga coordinates ui.screens.canvas responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("CanvasManga.input", "CanvasManga.state", "CanvasManga.output", "CanvasManga.failure", "CanvasManga.telemetry")

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
