package com.waheed.artificerx.core.art

import kotlin.math.hypot

/**
 * High-frequency preprocessing for finger/stylus strokes before rasterization.
 * The processor deliberately stays independent from Android/Compose so the same
 * geometry can be used by manual drawing, AI-generated paths, replay and tests.
 */
class AdvancedStrokeProcessor {
    data class Result(
        val points: List<Float>,
        val qualityScore: Float,
        val originalPointCount: Int,
        val outputPointCount: Int,
    )

    fun process(
        points: List<Float>,
        smoothing: Float,
        spacing: Float,
    ): Result {
        if (points.size < 4 || points.size % 2 != 0) {
            return Result(points, 0f, points.size / 2, points.size / 2)
        }
        val normalizedSmoothing = smoothing.coerceIn(0f, 1f)
        val normalizedSpacing = spacing.coerceIn(0.01f, 1f)
        val smoothed = smooth(points, normalizedSmoothing)
        val spaced = resample(smoothed, normalizedSpacing)
        val score = quality(smoothed, spaced)
        return Result(spaced, score, points.size / 2, spaced.size / 2)
    }

    private fun smooth(points: List<Float>, amount: Float): List<Float> {
        if (amount <= 0.001f || points.size < 6) return points.toList()
        val result = points.toMutableList()
        val radius = if (amount < 0.34f) 1 else if (amount < 0.7f) 2 else 3
        var i = 2
        while (i + 1 < points.size - 2) {
            var sx = 0f
            var sy = 0f
            var count = 0
            for (r in -radius..radius) {
                val index = i + r * 2
                if (index < 0 || index + 1 >= points.size) continue
                sx += points[index]
                sy += points[index + 1]
                count++
            }
            if (count > 0) {
                val targetX = sx / count
                val targetY = sy / count
                val originalX = points[i]
                val originalY = points[i + 1]
                result[i] = originalX + (targetX - originalX) * amount
                result[i + 1] = originalY + (targetY - originalY) * amount
            }
            i += 2
        }
        return result
    }

    private fun resample(points: List<Float>, spacing: Float): List<Float> {
        if (points.size < 4) return points
        val targetDistance = (0.75f + spacing * 7.5f).coerceAtLeast(0.75f)
        val result = ArrayList<Float>(points.size)
        result += points[0]
        result += points[1]
        var lastX = points[0]
        var lastY = points[1]
        var carry = 0f
        var i = 2
        while (i + 1 < points.size) {
            val currentX = points[i]
            val currentY = points[i + 1]
            val dx = currentX - lastX
            val dy = currentY - lastY
            val distance = hypot(dx, dy)
            if (distance + carry >= targetDistance && distance > 0.0001f) {
                val ratio = ((targetDistance - carry) / distance).coerceIn(0f, 1f)
                val x = lastX + dx * ratio
                val y = lastY + dy * ratio
                result += x
                result += y
                carry = 0f
                lastX = x
                lastY = y
                if (ratio < 0.999f) continue
            } else {
                carry += distance
            }
            lastX = currentX
            lastY = currentY
            i += 2
        }
        val endX = points[points.size - 2]
        val endY = points[points.size - 1]
        if (result[result.size - 2] != endX || result[result.size - 1] != endY) {
            result += endX
            result += endY
        }
        return result
    }

    private fun quality(before: List<Float>, after: List<Float>): Float {
        if (before.size < 4 || after.size < 4) return 0f
        val reduction = 1f - (after.size.toFloat() / before.size.toFloat())
        val continuity = continuityScore(after)
        return (continuity * 0.72f + reduction.coerceIn(0f, 1f) * 0.28f).coerceIn(0f, 1f)
    }

    private fun continuityScore(points: List<Float>): Float {
        var total = 0f
        var count = 0
        var i = 4
        while (i + 1 < points.size) {
            val ax = points[i - 2] - points[i - 4]
            val ay = points[i - 1] - points[i - 3]
            val bx = points[i] - points[i - 2]
            val by = points[i + 1] - points[i - 1]
            val aLen = hypot(ax, ay)
            val bLen = hypot(bx, by)
            if (aLen > 0.01f && bLen > 0.01f) {
                val cosine = ((ax * bx + ay * by) / (aLen * bLen)).coerceIn(-1f, 1f)
                total += (cosine + 1f) * 0.5f
                count++
            }
            i += 2
        }
        return if (count == 0) 0.5f else total / count
    }
}
