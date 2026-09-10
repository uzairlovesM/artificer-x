package com.waheed.artificerx.core.drawing

data class PressureCurve(val points: List<Pair<Float, Float>>) {
    init {
        require(points.isNotEmpty())
        require(points.all { it.first in 0f..1f && it.second in 0f..1f })
    }

    fun map(input: Float): Float {
        val x = input.coerceIn(0f, 1f)
        if (x <= points.first().first) return points.first().second
        for (i in 1 until points.size) {
            val (x2, y2) = points[i]
            val (x1, y1) = points[i - 1]
            if (x <= x2) {
                val t = if (x2 == x1) 0f else (x - x1) / (x2 - x1)
                return y1 + (y2 - y1) * t
            }
        }
        return points.last().second
    }

    companion object {
        val Linear = PressureCurve(listOf(0f to 0f, 1f to 1f))
        val Soft = PressureCurve(listOf(0f to 0f, .25f to .15f, .6f to .55f, 1f to 1f))
        val Firm = PressureCurve(listOf(0f to 0f, .25f to .35f, .6f to .72f, 1f to 1f))
    }
}
