package com.waheed.artificerx.core.render

/** Adapts render quality to a measurable frame-time budget instead of guessing from device class. */
class RenderBudgetController(
    private val targetFrameMillis: Double = 16.67,
    private val historySize: Int = 30,
) {
    init { require(targetFrameMillis > 0.0); require(historySize > 0) }

    data class BudgetState(
        val averageFrameMillis: Double,
        val worstFrameMillis: Double,
        val quality: Double,
        val droppedFrames: Int,
    )

    private val samples = ArrayDeque<Double>()
    private var dropped = 0

    @Synchronized fun recordFrame(durationMillis: Double, droppedFrame: Boolean = durationMillis > targetFrameMillis * 1.5): BudgetState {
        require(durationMillis >= 0.0)
        samples.addLast(durationMillis)
        while (samples.size > historySize) samples.removeFirst()
        if (droppedFrame) dropped++
        val avg = samples.average()
        val worst = samples.maxOrNull() ?: 0.0
        val pressure = (avg / targetFrameMillis).coerceIn(0.25, 3.0)
        val quality = (1.15 - (pressure - 0.75) * 0.5).coerceIn(0.25, 1.0)
        return BudgetState(avg, worst, quality, dropped)
    }

    @Synchronized fun reset() { samples.clear(); dropped = 0 }
}
