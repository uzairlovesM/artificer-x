package com.waheed.artificerx.core.insights

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for TrendAnalyzer. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class TrendAnalyzerExpansion : ExpansionCapability {
    override val id: String = "core.insights.trendanalyzer"
    override val area: String = "core.insights"
    override val purpose: String = "TrendAnalyzer coordinates core.insights responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("TrendAnalyzer.input", "TrendAnalyzer.state", "TrendAnalyzer.output", "TrendAnalyzer.failure", "TrendAnalyzer.telemetry")

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
