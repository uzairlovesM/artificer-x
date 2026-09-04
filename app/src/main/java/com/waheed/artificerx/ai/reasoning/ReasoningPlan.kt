package com.waheed.artificerx.ai.reasoning

data class ReasoningPlan(
    val goal: String,
    val assumptions: List<String>,
    val steps: List<ReasoningStep>,
    val verification: List<VerificationStep>,
    val fallbackStrategies: List<String>
)

data class ReasoningStep(val id: String, val action: String, val requiredCapabilities: Set<String>, val expectedEvidence: List<String>)
data class VerificationStep(val id: String, val predicate: String, val repairAction: String?)
