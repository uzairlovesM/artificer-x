package com.waheed.artificerx.core.terminal

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for ErrorCollector. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class ErrorCollectorExpansion : ExpansionCapability {
    override val id: String = "core.terminal.errorcollector"
    override val area: String = "core.terminal"
    override val purpose: String = "ErrorCollector coordinates core.terminal responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("ErrorCollector.input", "ErrorCollector.state", "ErrorCollector.output", "ErrorCollector.failure", "ErrorCollector.telemetry")

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
