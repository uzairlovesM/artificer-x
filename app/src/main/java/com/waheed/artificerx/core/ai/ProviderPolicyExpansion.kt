package com.waheed.artificerx.core.ai

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for ProviderPolicy. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class ProviderPolicyExpansion : ExpansionCapability {
    override val id: String = "core.ai.providerpolicy"
    override val area: String = "core.ai"
    override val purpose: String = "ProviderPolicy coordinates core.ai responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("ProviderPolicy.input", "ProviderPolicy.state", "ProviderPolicy.output", "ProviderPolicy.failure", "ProviderPolicy.telemetry")

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
