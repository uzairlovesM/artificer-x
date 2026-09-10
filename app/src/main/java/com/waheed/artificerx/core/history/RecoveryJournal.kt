package com.waheed.artificerx.core.history

import java.security.MessageDigest
import java.util.ArrayDeque

/** In-memory write-ahead journal. Persistence adapters can serialize Entry without changing callers. */
class RecoveryJournal(private val capacity: Int = 2048) {
    init { require(capacity > 0) }

    data class Entry(
        val sequence: Long,
        val operationId: String,
        val phase: Phase,
        val payload: String,
        val checksum: String,
        val timestampMillis: Long,
    )

    enum class Phase { BEGIN, APPLY, VERIFY, COMMIT, ROLLBACK, RECOVERED }

    private var sequence = 0L
    private val entries = ArrayDeque<Entry>()

    @Synchronized
    fun append(operationId: String, phase: Phase, payload: String): Entry {
        require(operationId.isNotBlank())
        val entry = Entry(++sequence, operationId, phase, payload, hash(sequence, operationId, phase, payload), System.currentTimeMillis())
        entries.addLast(entry)
        while (entries.size > capacity) entries.removeFirst()
        return entry
    }

    @Synchronized fun snapshot(): List<Entry> = entries.toList()

    @Synchronized fun incompleteOperations(): Set<String> {
        val phases = entries.groupBy { it.operationId }.mapValues { (_, values) -> values.lastOrNull()?.phase }
        return phases.filterValues { it == Phase.BEGIN || it == Phase.APPLY || it == Phase.VERIFY }.keys
    }

    private fun hash(sequence: Long, operationId: String, phase: Phase, payload: String): String {
        val input = "$sequence|$operationId|$phase|$payload"
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
