package com.waheed.artificerx.runtime

import com.waheed.artificerx.core.foundation.SubsystemHealth
import kotlin.math.min

class RuntimeGuardRuntime {
    data class Budget(val operations: Int, val bytes: Long, val millis: Long)
    data class Usage(val operations: Int, val bytes: Long, val millis: Long)

    fun allowed(budget: Budget, usage: Usage): Boolean =
        usage.operations <= budget.operations && usage.bytes <= budget.bytes && usage.millis <= budget.millis

    fun remaining(budget: Budget, usage: Usage): Budget = Budget(
        operations = (budget.operations - usage.operations).coerceAtLeast(0),
        bytes = (budget.bytes - usage.bytes).coerceAtLeast(0L),
        millis = (budget.millis - usage.millis).coerceAtLeast(0L),
    )

    fun backoff(attempt: Int, base: Long = 250, cap: Long = 30_000): Long = min(cap, base * (1L shl attempt.coerceIn(0, 16)))

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "runtime",
        readiness = 1.0,
        capabilities = setOf("budgeting", "remaining-capacity", "bounded-backoff"),
        invariants = listOf("non-negative-remaining-budget", "bounded-retry-delay"),
        counters = emptyMap(),
    )
}
