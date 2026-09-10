package com.waheed.artificerx.core.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

data class StateVersioned<T>(val version: Long, val value: T)

class VersionedStateStore<T>(initial: T) {
    private val sequence = AtomicLong(0L)
    private val state = MutableStateFlow(StateVersioned(0L, initial))
    val updates: StateFlow<StateVersioned<T>> = state.asStateFlow()

    @Synchronized
    fun update(transform: (T) -> T): StateVersioned<T> {
        val current = state.value
        val next = StateVersioned(sequence.incrementAndGet(), transform(current.value))
        state.value = next
        return next
    }

    @Synchronized
    fun replace(expectedVersion: Long, value: T): Boolean {
        if (state.value.version != expectedVersion) return false
        state.value = StateVersioned(sequence.incrementAndGet(), value)
        return true
    }

    fun snapshot(): StateVersioned<T> = state.value
}
