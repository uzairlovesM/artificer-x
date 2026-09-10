package com.waheed.artificerx.core.foundation

import java.util.concurrent.CopyOnWriteArrayList

/** Thread-safe in-memory journal used by foundation components before persistence is attached. */
class MutationJournal(private val maxEntries: Int = 2_000) {
    init { require(maxEntries > 0) }

    data class Entry(
        val sequence: Long,
        val executionId: ExecutionId,
        val correlationId: CorrelationId?,
        val action: String,
        val timestampMillis: Long,
        val payload: Map<String, String>,
    )

    private val sequence = java.util.concurrent.atomic.AtomicLong()
    private val entries = CopyOnWriteArrayList<Entry>()

    fun append(
        executionId: ExecutionId,
        action: String,
        correlationId: CorrelationId? = null,
        timestampMillis: Long = System.currentTimeMillis(),
        payload: Map<String, String> = emptyMap(),
    ): Entry {
        require(action.isNotBlank())
        val entry = Entry(sequence.incrementAndGet(), executionId, correlationId, action, timestampMillis, payload.toMap())
        entries.add(entry)
        while (entries.size > maxEntries) entries.removeAt(0)
        return entry
    }

    fun snapshot(): List<Entry> = entries.toList()

    fun entriesFor(executionId: ExecutionId): List<Entry> = entries.filter { it.executionId == executionId }

    fun clear() = entries.clear()
}
