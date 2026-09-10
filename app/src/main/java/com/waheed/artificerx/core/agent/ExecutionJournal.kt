package com.waheed.artificerx.core.agent

data class JournalEntry(
    val sequence: Long,
    val tool: String,
    val startedAt: Long,
    val finishedAt: Long,
    val success: Boolean,
    val summary: String
)

class ExecutionJournal(private val capacity: Int = 1_000) {
    private var sequence = 0L
    private val entries = ArrayDeque<JournalEntry>()

    @Synchronized
    fun record(tool: String, startedAt: Long, finishedAt: Long, success: Boolean, summary: String) {
        entries.addLast(JournalEntry(++sequence, tool, startedAt, finishedAt, success, summary.take(2_000)))
        while (entries.size > capacity) entries.removeFirst()
    }

    @Synchronized fun recent(limit: Int = 50): List<JournalEntry> =
        entries.takeLast(limit.coerceIn(1, capacity))
}
