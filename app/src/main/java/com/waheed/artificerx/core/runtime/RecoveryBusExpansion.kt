package com.waheed.artificerx.core.runtime

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for RecoveryBus. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class RecoveryBusExpansion : ExpansionCapability {
    override val id: String = "core.runtime.recoverybus"
    override val area: String = "core.runtime"
    override val purpose: String = "RecoveryBus coordinates core.runtime responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("RecoveryBus.input", "RecoveryBus.state", "RecoveryBus.output", "RecoveryBus.failure", "RecoveryBus.telemetry")

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
