package com.waheed.artificerx.core.telemetry

data class TelemetryEvent(
    val name: String,
    val timestampMillis: Long,
    val attributes: Map<String, String> = emptyMap()
)

class TelemetryBuffer(private val capacity: Int = 500) {
    private val events = ArrayDeque<TelemetryEvent>()
    @Synchronized fun add(event: TelemetryEvent) {
        events.addLast(event)
        while (events.size > capacity) events.removeFirst()
    }
    @Synchronized fun drain(): List<TelemetryEvent> = events.toList().also { events.clear() }
}
