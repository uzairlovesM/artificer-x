package com.waheed.artificerx.di

import com.waheed.artificerx.core.foundation.SubsystemHealth

class DiRuntime {
    data class Binding(val key: String, val implementation: String, val singleton: Boolean = false)
    fun validate(bindings: List<Binding>): List<String> {
        val errors = mutableListOf<String>()
        bindings.groupBy { it.key }.forEach { (key, values) ->
            if (values.size > 1 && values.count { it.singleton } > 1) errors += "duplicate-singleton:$key"
            if (values.any { it.implementation.isBlank() }) errors += "blank-implementation:$key"
        }
        return errors
    }
    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "di",
        readiness = 1.0,
        capabilities = setOf("binding-audit", "singleton-conflict-detection"),
        invariants = listOf("no-duplicate-singleton-binding"),
        counters = emptyMap(),
    )
}
