package com.waheed.artificerx.core.runtime

import kotlin.math.min
import kotlin.random.Random

data class RetryDecision(val attempt: Int, val delayMillis: Long)

class RetryPolicy(
    private val maxAttempts: Int = 5,
    private val initialDelayMillis: Long = 250L,
    private val maxDelayMillis: Long = 8_000L,
    private val jitter: Double = 0.20,
    private val random: Random = Random.Default
) {
    init {
        require(maxAttempts >= 1)
        require(initialDelayMillis >= 0 && maxDelayMillis >= initialDelayMillis)
        require(jitter in 0.0..1.0)
    }

    fun decisions(): Sequence<RetryDecision> = sequence {
        for (attempt in 1..maxAttempts) {
            val base = min(maxDelayMillis, initialDelayMillis * (1L shl min(20, attempt - 1)))
            val factor = 1.0 + ((random.nextDouble() * 2.0 - 1.0) * jitter)
            yield(RetryDecision(attempt, (base * factor).toLong().coerceAtLeast(0L)))
        }
    }
}
