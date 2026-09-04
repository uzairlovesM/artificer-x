package com.waheed.artificerx.core.render

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for ToneRenderer. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class ToneRendererExpansion : ExpansionCapability {
    override val id: String = "core.render.tonerenderer"
    override val area: String = "core.render"
    override val purpose: String = "ToneRenderer coordinates core.render responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("ToneRenderer.input", "ToneRenderer.state", "ToneRenderer.output", "ToneRenderer.failure", "ToneRenderer.telemetry")

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
