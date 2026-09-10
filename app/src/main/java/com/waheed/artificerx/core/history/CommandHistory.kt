package com.waheed.artificerx.core.history

data class HistoryEntry<T>(val label: String, val before: T, val after: T, val timestamp: Long)

class CommandHistory<T>(
    private val capacity: Int = 200,
    private val clock: () -> Long = System::currentTimeMillis
) {
    init { require(capacity > 0) }
    private val undoStack = ArrayDeque<HistoryEntry<T>>()
    private val redoStack = ArrayDeque<HistoryEntry<T>>()

    @Synchronized
    fun push(label: String, before: T, after: T) {
        if (before == after) return
        undoStack.addLast(HistoryEntry(label, before, after, clock()))
        while (undoStack.size > capacity) undoStack.removeFirst()
        redoStack.clear()
    }

    @Synchronized fun undo(current: T): T? {
        val entry = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(entry)
        return entry.before
    }

    @Synchronized fun redo(current: T): T? {
        val entry = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(entry)
        return entry.after
    }

    @Synchronized fun canUndo() = undoStack.isNotEmpty()
    @Synchronized fun canRedo() = redoStack.isNotEmpty()
    @Synchronized fun clear() { undoStack.clear(); redoStack.clear() }
}
