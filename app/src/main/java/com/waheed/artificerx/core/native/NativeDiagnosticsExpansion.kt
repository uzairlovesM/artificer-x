package com.waheed.artificerx.core.native

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for NativeDiagnostics. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class NativeDiagnosticsExpansion : ExpansionCapability {
    override val id: String = "core.native.nativediagnostics"
    override val area: String = "core.native"
    override val purpose: String = "NativeDiagnostics coordinates core.native responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("NativeDiagnostics.input", "NativeDiagnostics.state", "NativeDiagnostics.output", "NativeDiagnostics.failure", "NativeDiagnostics.telemetry")

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
