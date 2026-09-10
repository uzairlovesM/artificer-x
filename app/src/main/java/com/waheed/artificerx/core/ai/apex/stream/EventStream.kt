package com.waheed.artificerx.core.ai.apex.stream

data class RuntimeEvent(val type: String, val sequence: Long, val payload: Map<String, String>)
class EventStream(private val capacity: Int = 4_000) {
    private val events = ArrayDeque<RuntimeEvent>()
    private var sequence = 0L
    @Synchronized fun emit(type: String, payload: Map<String, String> = emptyMap()): RuntimeEvent { val event = RuntimeEvent(type, ++sequence, payload); events.add(event); while (events.size > capacity) events.removeFirst(); return event }
    @Synchronized fun snapshot(): List<RuntimeEvent> = events.toList()
}
