package com.waheed.artificerx.data.remote.adapter

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for CircuitBreaker. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class CircuitBreakerExpansion : ExpansionCapability {
    override val id: String = "data.remote.adapter.circuitbreaker"
    override val area: String = "data.remote.adapter"
    override val purpose: String = "CircuitBreaker coordinates data.remote.adapter responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("CircuitBreaker.input", "CircuitBreaker.state", "CircuitBreaker.output", "CircuitBreaker.failure", "CircuitBreaker.telemetry")

    override fun validate(): CapabilityCheck {
        val signalCount = contracts.count { it.isNotBlank() }
        return CapabilityCheck(
            id = id,
            ready = signalCount == contracts.size && contracts.isNotEmpty(),
            reason = if (signalCount == contracts.size) "contracts-ready" else "contract-gap",
            signals = mapOf(
                "contractCount" to signalCount.toString(),
                "area" to area,
                "purposeLength" to purpose.length.toString(),
            ),
        )
    }
}
