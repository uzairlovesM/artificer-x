package com.waheed.artificerx.core.history

import java.util.ArrayDeque
import java.util.UUID

/** Transactional history independent from UI or a particular bitmap implementation. */
class CanvasHistoryEngine(private val capacity: Int = 1024) {
    init { require(capacity > 0) }

    data class Revision(
        val id: String = UUID.randomUUID().toString(),
        val label: String,
        val beforeFingerprint: String,
        val afterFingerprint: String,
        val timestampMillis: Long = System.currentTimeMillis(),
        val actor: String = "user",
        val reversible: Boolean = true,
        val metadata: Map<String, String> = emptyMap(),
    )

    private val undo = ArrayDeque<Revision>()
    private val redo = ArrayDeque<Revision>()

    @Synchronized fun record(revision: Revision) {
        require(revision.label.isNotBlank())
        require(revision.beforeFingerprint != revision.afterFingerprint) { "No-op revision cannot enter history" }
        undo.addLast(revision)
        redo.clear()
        trim()
    }

    @Synchronized fun undo(): Revision? {
        if (undo.isEmpty()) return null
        val item = undo.removeLast()
        if (item.reversible) redo.addLast(item)
        return item
    }

    @Synchronized fun redo(): Revision? {
        if (redo.isEmpty()) return null
        val item = redo.removeLast()
        undo.addLast(item)
        return item
    }

    @Synchronized fun peekUndo(): Revision? = undo.lastOrNull()
    @Synchronized fun peekRedo(): Revision? = redo.lastOrNull()
    @Synchronized fun undoCount(): Int = undo.size
    @Synchronized fun redoCount(): Int = redo.size
    @Synchronized fun snapshot(): Pair<List<Revision>, List<Revision>> = undo.toList() to redo.toList()
    @Synchronized fun clear() { undo.clear(); redo.clear() }

    private fun trim() { while (undo.size > capacity) undo.removeFirst() }
}
