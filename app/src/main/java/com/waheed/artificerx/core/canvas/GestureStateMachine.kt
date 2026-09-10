package com.waheed.artificerx.core.canvas

/** Deterministic gesture recognizer state used by the canvas UI and tests. */
class GestureStateMachine {
    enum class Mode { IDLE, DRAWING, PANNING, ZOOMING, ROTATING, TRANSFORMING }
    data class Pointer(val id: Int, val x: Float, val y: Float)
    data class State(val mode: Mode = Mode.IDLE, val pointers: List<Pointer> = emptyList(), val sequence: Long = 0L)

    private var state = State()

    @Synchronized fun state(): State = state

    @Synchronized fun down(pointer: Pointer, drawingEnabled: Boolean, transformEnabled: Boolean = false): State {
        val next = state.pointers.filterNot { it.id == pointer.id } + pointer
        state = state.copy(
            mode = when {
                next.size >= 2 && transformEnabled -> Mode.TRANSFORMING
                next.size >= 2 -> Mode.ZOOMING
                drawingEnabled -> Mode.DRAWING
                else -> Mode.PANNING
            },
            pointers = next,
            sequence = state.sequence + 1,
        )
        return state
    }

    @Synchronized fun move(pointer: Pointer): State {
        if (state.pointers.none { it.id == pointer.id }) return state
        state = state.copy(pointers = state.pointers.map { if (it.id == pointer.id) pointer else it }, sequence = state.sequence + 1)
        return state
    }

    @Synchronized fun up(pointerId: Int): State {
        val remaining = state.pointers.filterNot { it.id == pointerId }
        state = state.copy(mode = if (remaining.isEmpty()) Mode.IDLE else state.mode, pointers = remaining, sequence = state.sequence + 1)
        return state
    }

    @Synchronized fun cancel(): State { state = State(sequence = state.sequence + 1); return state }
}
