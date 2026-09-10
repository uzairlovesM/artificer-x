package com.waheed.artificerx.core.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

data class EventEnvelope(
    val id: Long,
    val type: String,
    val timestampMillis: Long,
    val payload: Map<String, String> = emptyMap()
)

class EventHub(private val clock: () -> Long = System::currentTimeMillis) {
    private val id = AtomicLong()
    private val stream = MutableSharedFlow<EventEnvelope>(extraBufferCapacity = 256)
    private val counters = ConcurrentHashMap<String, AtomicLong>()

    val events: SharedFlow<EventEnvelope> = stream.asSharedFlow()

    fun publish(type: String, payload: Map<String, String> = emptyMap()): EventEnvelope {
        require(type.isNotBlank()) { "Event type must not be blank" }
        val event = EventEnvelope(id.incrementAndGet(), type, clock(), payload.toMap())
        counters.computeIfAbsent(type) { AtomicLong() }.incrementAndGet()
        stream.tryEmit(event)
        return event
    }

    fun count(type: String): Long = counters[type]?.get() ?: 0L
    fun clearCounters() = counters.clear()
}
