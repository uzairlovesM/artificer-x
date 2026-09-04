package com.waheed.artificerx.core.automation

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for Lease. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class LeaseExpansion : ExpansionCapability {
    override val id: String = "core.automation.lease"
    override val area: String = "core.automation"
    override val purpose: String = "Lease coordinates core.automation responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("Lease.input", "Lease.state", "Lease.output", "Lease.failure", "Lease.telemetry")

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
