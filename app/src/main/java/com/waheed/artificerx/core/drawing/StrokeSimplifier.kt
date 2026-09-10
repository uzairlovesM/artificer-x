package com.waheed.artificerx.core.drawing

import kotlin.math.abs
import kotlin.math.hypot

object StrokeSimplifier {
    fun simplify(points: List<StrokePoint>, tolerance: Float): List<StrokePoint> {
        require(tolerance >= 0f)
        if (points.size < 3) return points
        val keep = BooleanArray(points.size)
        keep[0] = true; keep[points.lastIndex] = true
        simplifyRange(points, 0, points.lastIndex, tolerance * tolerance, keep)
        return points.indices.filter { keep[it] }.map(points::get)
    }

    private fun simplifyRange(
        points: List<StrokePoint>, start: Int, end: Int, toleranceSquared: Float, keep: BooleanArray
    ) {
        if (end <= start + 1) return
        val a = points[start]; val b = points[end]
        var maxDistance = toleranceSquared
        var index = -1
        for (i in start + 1 until end) {
            val distance = squaredDistanceToSegment(points[i], a, b)
            if (distance > maxDistance) { maxDistance = distance; index = i }
        }
        if (index >= 0) {
            keep[index] = true
            simplifyRange(points, start, index, toleranceSquared, keep)
            simplifyRange(points, index, end, toleranceSquared, keep)
        }
    }

    private fun squaredDistanceToSegment(p: StrokePoint, a: StrokePoint, b: StrokePoint): Float {
        val dx = b.x - a.x; val dy = b.y - a.y
        if (abs(dx) < 0.00001f && abs(dy) < 0.00001f)
            return hypot((p.x - a.x).toDouble(), (p.y - a.y).toDouble()).toFloat().let { it * it }
        val t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy)
        val clamped = t.coerceIn(0f, 1f)
        val x = a.x + clamped * dx; val y = a.y + clamped * dy
        val ex = p.x - x; val ey = p.y - y
        return ex * ex + ey * ey
    }
}
