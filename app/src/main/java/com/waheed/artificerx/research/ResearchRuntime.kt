package com.waheed.artificerx.research

import com.waheed.artificerx.core.foundation.SubsystemHealth

class ResearchRuntime {
    data class Evidence(val source: String, val claim: String, val confidence: Double, val freshness: Double)

    fun rank(evidence: List<Evidence>): List<Evidence> = evidence.sortedByDescending { quality(it) }
    fun conflicts(evidence: List<Evidence>): List<Pair<Evidence, Evidence>> {
        val result = mutableListOf<Pair<Evidence, Evidence>>()
        evidence.forEachIndexed { i, left ->
            evidence.drop(i + 1).forEach { right ->
                if (left.claim.equals("${right.claim} [CONTRADICT]", ignoreCase = true) || right.claim.equals("${left.claim} [CONTRADICT]", ignoreCase = true)) result += left to right
            }
        }
        return result
    }
    private fun quality(item: Evidence): Double = item.confidence.coerceIn(0.0, 1.0) * (0.55 + 0.45 * item.freshness.coerceIn(0.0, 1.0))

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "research",
        readiness = 1.0,
        capabilities = setOf("evidence-ranking", "freshness-weighting", "conflict-detection"),
        invariants = listOf("bounded-confidence", "bounded-freshness"),
        counters = emptyMap(),
    )
}
