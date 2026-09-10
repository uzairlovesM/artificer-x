package com.waheed.artificerx.core.ai.apex.reasoning

data class ReasoningPolicy(val maxSteps: Int = 64, val minConfidence: Double = 0.55, val allowRepair: Boolean = true, val requireEvidence: Boolean = false) {
    init { require(maxSteps > 0); require(minConfidence in 0.0..1.0) }
}

class ReasoningPolicyEngine(private var policy: ReasoningPolicy = ReasoningPolicy()) {
    fun update(next: ReasoningPolicy) { policy = next }
    fun snapshot(): ReasoningPolicy = policy
    fun shouldAccept(confidence: Double, evidenceCount: Int): Boolean = confidence >= policy.minConfidence && (!policy.requireEvidence || evidenceCount > 0)
}
