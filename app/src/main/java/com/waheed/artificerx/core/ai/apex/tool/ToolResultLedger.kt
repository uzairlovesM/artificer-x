package com.waheed.artificerx.core.ai.apex.tool

data class ToolLedgerEntry(val sequence: Long, val request: ToolRequest, val result: ToolResult)
class ToolResultLedger(private val capacity: Int = 2_000) {
    private val entries = ArrayDeque<ToolLedgerEntry>()
    private var sequence = 0L
    @Synchronized fun record(request: ToolRequest, result: ToolResult): ToolLedgerEntry { val entry = ToolLedgerEntry(++sequence, request, result); entries.add(entry); while (entries.size > capacity) entries.removeFirst(); return entry }
    @Synchronized fun recent(limit: Int = 50): List<ToolLedgerEntry> = entries.takeLast(limit.coerceAtLeast(0))
}
