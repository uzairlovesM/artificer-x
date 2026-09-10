package com.waheed.artificerx.core.ai.planning

data class TokenBudget(val maximum: Int, val reserved: Int = 0) {
    init { require(maximum > 0); require(reserved in 0..maximum) }
    val available: Int get() = maximum - reserved
    fun reserve(amount: Int): TokenBudget = copy(reserved = (reserved + amount).coerceAtMost(maximum))
    fun release(amount: Int): TokenBudget = copy(reserved = (reserved - amount).coerceAtLeast(0))
}

class BudgetPlanner(private val defaultBudget: TokenBudget) {
    fun allocate(
        inputTokens: Int,
        toolTokens: Int,
        outputTokens: Int,
        safetyReserve: Int = defaultBudget.maximum / 10
    ): TokenBudget {
        val used = (inputTokens + toolTokens).coerceAtLeast(0)
        val available = (defaultBudget.maximum - used - safetyReserve).coerceAtLeast(0)
        return TokenBudget(defaultBudget.maximum, defaultBudget.maximum - available)
            .let { it.copy(reserved = (used + safetyReserve).coerceAtMost(it.maximum)) }
            .let { if (outputTokens > it.available) it.copy(reserved = (it.maximum - outputTokens.coerceAtMost(it.maximum)).coerceAtLeast(0)) else it }
    }
}
