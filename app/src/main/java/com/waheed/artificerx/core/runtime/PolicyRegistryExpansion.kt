package com.waheed.artificerx.core.runtime

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for PolicyRegistry. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class PolicyRegistryExpansion : ExpansionCapability {
    override val id: String = "core.runtime.policyregistry"
    override val area: String = "core.runtime"
    override val purpose: String = "PolicyRegistry coordinates core.runtime responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("PolicyRegistry.input", "PolicyRegistry.state", "PolicyRegistry.output", "PolicyRegistry.failure", "PolicyRegistry.telemetry")

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
