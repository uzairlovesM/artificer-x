package com.waheed.artificerx.data.remote.middleware

data class RetryPolicy(val maxAttempts: Int = 3, val initialDelayMs: Long = 400L, val multiplier: Double = 2.0, val maxDelayMs: Long = 8_000L) {
    fun delayFor(attempt: Int): Long = (initialDelayMs * multiplier.pow(attempt.coerceAtLeast(0))).toLong().coerceAtMost(maxDelayMs)
    private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())
}
