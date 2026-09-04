package com.waheed.artificerx.core.insights

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for ThermalAnalyzer. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class ThermalAnalyzerExpansion : ExpansionCapability {
    override val id: String = "core.insights.thermalanalyzer"
    override val area: String = "core.insights"
    override val purpose: String = "ThermalAnalyzer coordinates core.insights responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("ThermalAnalyzer.input", "ThermalAnalyzer.state", "ThermalAnalyzer.output", "ThermalAnalyzer.failure", "ThermalAnalyzer.telemetry")

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
