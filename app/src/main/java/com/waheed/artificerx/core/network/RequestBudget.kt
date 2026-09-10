package com.waheed.artificerx.core.network

class RequestBudget(
    private val maxRequests: Int,
    private val windowMillis: Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    init { require(maxRequests > 0); require(windowMillis > 0) }
    private var windowStart = clock()
    private var count = 0

    @Synchronized
    fun tryAcquire(): Boolean {
        val now = clock()
        if (now - windowStart >= windowMillis) { windowStart = now; count = 0 }
        if (count >= maxRequests) return false
        count++
        return true
    }

    @Synchronized fun remaining(): Int {
        if (clock() - windowStart >= windowMillis) return maxRequests
        return (maxRequests - count).coerceAtLeast(0)
    }
}
