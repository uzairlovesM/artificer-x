package com.waheed.artificerx.core.insights

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for ReadinessAnalyzer. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class ReadinessAnalyzerExpansion : ExpansionCapability {
    override val id: String = "core.insights.readinessanalyzer"
    override val area: String = "core.insights"
    override val purpose: String = "ReadinessAnalyzer coordinates core.insights responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("ReadinessAnalyzer.input", "ReadinessAnalyzer.state", "ReadinessAnalyzer.output", "ReadinessAnalyzer.failure", "ReadinessAnalyzer.telemetry")

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
