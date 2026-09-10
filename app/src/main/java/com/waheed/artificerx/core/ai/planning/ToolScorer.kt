package com.waheed.artificerx.core.ai.planning

data class ToolCandidate(
    val name: String,
    val reliability: Float,
    val latencyMillis: Long,
    val risk: Float,
    val relevance: Float
)

class ToolScorer {
    fun rank(candidates: List<ToolCandidate>): List<ToolCandidate> =
        candidates.sortedByDescending {
            val latencyPenalty = (it.latencyMillis.coerceAtMost(10_000L) / 10_000f) * .12f
            (it.relevance.coerceIn(0f,1f) * .55f) +
                (it.reliability.coerceIn(0f,1f) * .30f) -
                (it.risk.coerceIn(0f,1f) * .25f) - latencyPenalty
        }
}
