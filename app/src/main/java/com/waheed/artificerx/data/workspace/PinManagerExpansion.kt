package com.waheed.artificerx.data.workspace

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for PinManager. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class PinManagerExpansion : ExpansionCapability {
    override val id: String = "data.workspace.pinmanager"
    override val area: String = "data.workspace"
    override val purpose: String = "PinManager coordinates data.workspace responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("PinManager.input", "PinManager.state", "PinManager.output", "PinManager.failure", "PinManager.telemetry")

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
