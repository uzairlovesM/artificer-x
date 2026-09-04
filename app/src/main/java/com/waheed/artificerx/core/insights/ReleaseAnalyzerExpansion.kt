package com.waheed.artificerx.core.insights

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for ReleaseAnalyzer. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class ReleaseAnalyzerExpansion : ExpansionCapability {
    override val id: String = "core.insights.releaseanalyzer"
    override val area: String = "core.insights"
    override val purpose: String = "ReleaseAnalyzer coordinates core.insights responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("ReleaseAnalyzer.input", "ReleaseAnalyzer.state", "ReleaseAnalyzer.output", "ReleaseAnalyzer.failure", "ReleaseAnalyzer.telemetry")

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
