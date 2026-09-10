package com.waheed.artificerx.core.drawing.engine

class DrawingCommandHistory(private val capacity: Int = 512) {
    private val undo = ArrayDeque<DrawingCommand>()
    private val redo = ArrayDeque<DrawingCommand>()

    val undoCount: Int get() = undo.size
    val redoCount: Int get() = redo.size

    fun push(command: DrawingCommand) {
        undo.add(command)
        while (undo.size > capacity.coerceAtLeast(1)) undo.removeFirst()
        redo.clear()
    }

    fun undo(): DrawingCommand? = if (undo.isEmpty()) null else undo.removeLast().also { redo.add(it) }
    fun redo(): DrawingCommand? = if (redo.isEmpty()) null else redo.removeLast().also { undo.add(it) }
    fun clear() { undo.clear(); redo.clear() }
}
