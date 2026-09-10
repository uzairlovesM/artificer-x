package com.waheed.artificerx.core.ai.apex.vision

import kotlin.math.abs

data class VisualDiffMetrics(val meanAbsoluteError: Double, val changedPixels: Long, val totalPixels: Long, val changedRatio: Double, val similarity: Double)

class VisualDiffEngine {
    fun compare(before: IntArray, after: IntArray): VisualDiffMetrics {
        require(before.size == after.size)
        if (before.isEmpty()) return VisualDiffMetrics(0.0, 0, 0, 0.0, 1.0)
        var error = 0.0
        var changed = 0L
        for (i in before.indices) {
            val a = before[i]; val b = after[i]
            val dr = abs(((a ushr 16) and 0xff) - ((b ushr 16) and 0xff))
            val dg = abs(((a ushr 8) and 0xff) - ((b ushr 8) and 0xff))
            val db = abs((a and 0xff) - (b and 0xff))
            val da = abs(((a ushr 24) and 0xff) - ((b ushr 24) and 0xff))
            val d = (dr + dg + db + da) / 1020.0
            error += d
            if (d > 0.01) changed++
        }
        val ratio = changed.toDouble() / before.size
        val mae = error / before.size
        return VisualDiffMetrics(mae, changed, before.size.toLong(), ratio, (1.0 - mae).coerceIn(0.0, 1.0))
    }
}
