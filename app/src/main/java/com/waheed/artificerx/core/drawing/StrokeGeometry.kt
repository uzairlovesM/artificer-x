package com.waheed.artificerx.core.drawing

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val tilt: Float = 0f,
    val timestamp: Long = 0L
)

data class Bounds(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = max(0f, right - left)
    val height get() = max(0f, bottom - top)
    fun expand(amount: Float) = Bounds(left - amount, top - amount, right + amount, bottom + amount)
}

object StrokeGeometry {
    fun length(points: List<StrokePoint>): Float {
        var total = 0f
        for (i in 1 until points.size) {
            val a = points[i - 1]; val b = points[i]
            total += hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
        }
        return total
    }

    fun bounds(points: List<StrokePoint>): Bounds? {
        if (points.isEmpty()) return null
        var l = points[0].x; var r = l; var t = points[0].y; var b = t
        for (p in points.drop(1)) {
            l = min(l, p.x); r = max(r, p.x); t = min(t, p.y); b = max(b, p.y)
        }
        return Bounds(l, t, r, b)
    }

    fun resample(points: List<StrokePoint>, maxDistance: Float): List<StrokePoint> {
        require(maxDistance > 0f)
        if (points.size < 2) return points
        val out = ArrayList<StrokePoint>(points.size)
        out += points.first()
        for (point in points.drop(1)) {
            val previous = out.last()
            val distance = hypot((point.x - previous.x).toDouble(), (point.y - previous.y).toDouble()).toFloat()
            if (distance >= maxDistance) {
                val steps = max(1, (distance / maxDistance).toInt())
                for (s in 1..steps) {
                    val f = min(1f, s.toFloat() / steps)
                    out += StrokePoint(
                        previous.x + (point.x - previous.x) * f,
                        previous.y + (point.y - previous.y) * f,
                        previous.pressure + (point.pressure - previous.pressure) * f,
                        previous.tilt + (point.tilt - previous.tilt) * f,
                        previous.timestamp + ((point.timestamp - previous.timestamp) * f).toLong()
                    )
                }
            } else if (point !== points.last()) {
                out += point
            }
        }
        if (out.last() != points.last()) out += points.last()
        return out
    }
}
