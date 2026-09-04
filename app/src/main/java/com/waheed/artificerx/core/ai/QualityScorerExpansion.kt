package com.waheed.artificerx.core.ai

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for QualityScorer. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class QualityScorerExpansion : ExpansionCapability {
    override val id: String = "core.ai.qualityscorer"
    override val area: String = "core.ai"
    override val purpose: String = "QualityScorer coordinates core.ai responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("QualityScorer.input", "QualityScorer.state", "QualityScorer.output", "QualityScorer.failure", "QualityScorer.telemetry")

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
