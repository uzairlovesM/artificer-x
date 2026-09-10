package com.waheed.artificerx.core.ai.apex.stream

data class ContinuationPolicy(val maxContinuations: Int = 16, val minProgressChars: Int = 32)
class ContinuationController(private val policy: ContinuationPolicy = ContinuationPolicy()) {
    fun shouldContinue(text: String, stopReason: String?, continuationCount: Int): Boolean = continuationCount < policy.maxContinuations && stopReason in setOf("length", "max_tokens", "context") && text.trim().length >= policy.minProgressChars
    fun nextPrompt(previous: String): String = "Continue from the last completed point without repeating existing content. Preserve structure and constraints. Last output:\n${previous.takeLast(8_000)}"
}
