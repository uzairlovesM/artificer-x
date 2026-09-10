package com.waheed.artificerx.core.runtime

import kotlin.math.min

enum class CircuitState { CLOSED, OPEN, HALF_OPEN }

class CircuitBreaker(
    private val failureThreshold: Int = 4,
    private val openMillis: Long = 15_000L,
    private val clock: () -> Long = System::currentTimeMillis
) {
    init {
        require(failureThreshold > 0)
        require(openMillis > 0)
    }

    private var state = CircuitState.CLOSED
    private var failures = 0
    private var openedAt = 0L

    @Synchronized fun state(): CircuitState {
        if (state == CircuitState.OPEN && clock() - openedAt >= openMillis) state = CircuitState.HALF_OPEN
        return state
    }

    @Synchronized fun allowRequest(): Boolean = when (state()) {
        CircuitState.CLOSED, CircuitState.HALF_OPEN -> true
        CircuitState.OPEN -> false
    }

    @Synchronized fun recordSuccess() {
        failures = 0
        state = CircuitState.CLOSED
    }

    @Synchronized fun recordFailure() {
        failures = min(failureThreshold, failures + 1)
        if (failures >= failureThreshold) {
            state = CircuitState.OPEN
            openedAt = clock()
        }
    }

    @Synchronized fun failureCount(): Int = failures
}
