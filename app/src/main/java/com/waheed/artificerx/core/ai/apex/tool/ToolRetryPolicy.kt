package com.waheed.artificerx.core.ai.apex.tool

data class ToolRetryPolicy(val attempts: Int = 3, val baseDelayMs: Long = 50L, val multiplier: Double = 2.0) {
    init { require(attempts > 0); require(baseDelayMs >= 0); require(multiplier >= 1.0) }
    fun delayFor(attempt: Int): Long = (baseDelayMs * Math.pow(multiplier, attempt.coerceAtLeast(0).toDouble())).toLong()
}
