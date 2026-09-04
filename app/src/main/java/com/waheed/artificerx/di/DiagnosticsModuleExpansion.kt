package com.waheed.artificerx.di

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for DiagnosticsModule. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class DiagnosticsModuleExpansion : ExpansionCapability {
    override val id: String = "di.diagnosticsmodule"
    override val area: String = "di"
    override val purpose: String = "DiagnosticsModule coordinates di responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("DiagnosticsModule.input", "DiagnosticsModule.state", "DiagnosticsModule.output", "DiagnosticsModule.failure", "DiagnosticsModule.telemetry")

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
