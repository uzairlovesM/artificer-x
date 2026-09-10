package com.waheed.artificerx.core.timelapse

data class FrameIndex(val index: Int, val timestampMillis: Long)

class FrameTimeline {
    private val frames = ArrayList<FrameIndex>()
    fun append(timestampMillis: Long): FrameIndex {
        val frame = FrameIndex(frames.size, timestampMillis)
        frames += frame
        return frame
    }
    fun frameAt(index: Int): FrameIndex? = frames.getOrNull(index)
    fun all(): List<FrameIndex> = frames.toList()
    fun durationMillis(): Long = if (frames.size < 2) 0 else frames.last().timestampMillis - frames.first().timestampMillis
}
