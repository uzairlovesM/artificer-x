package com.waheed.artificerx.core.runtime
import org.junit.Assert.*
import org.junit.Test

class CircuitBreakerTest {
    @Test fun opensAfterThreshold() {
        var now = 0L
        val breaker = CircuitBreaker(2, 1000, { now })
        breaker.recordFailure()
        assertTrue(breaker.allowRequest())
        breaker.recordFailure()
        assertFalse(breaker.allowRequest())
        now = 1001
        assertTrue(breaker.allowRequest())
    }
}
