package com.waheed.artificerx.core.ai.apex.vision

data class RegionCandidate(val id: String, val area: Float, val edgeDensity: Float, val centerDistance: Float, val confidence: Float)
class RegionScorer {
    fun rank(candidates: Collection<RegionCandidate>): List<RegionCandidate> = candidates.sortedByDescending { it.confidence * 0.55f + it.edgeDensity * 0.25f + (1f - it.centerDistance.coerceIn(0f, 1f)) * 0.20f }
}
