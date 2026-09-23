package com.waheed.artificerx.core.render

import android.graphics.Bitmap
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Cheap, deterministic semantic inspection for agent-facing canvas snapshots.
 *
 * The bitmap is sampled instead of walked pixel-by-pixel. The result is
 * descriptive, not a replacement for the native raster analyser: dimensions,
 * alpha coverage, approximate occupied bounds, average colour, and luminance
 * are enough for an agent to decide whether a render pass produced meaningful
 * content without allocating a second full-size bitmap.
 */
object BitmapInspection {
    private const val MAX_SAMPLED_PIXELS = 20_000

    data class Summary(
        val width: Int,
        val height: Int,
        val sampledPixels: Int,
        val alphaCoverage: Float,
        val boundsLeft: Int?,
        val boundsTop: Int?,
        val boundsRight: Int?,
        val boundsBottom: Int?,
        val averageColorHex: String,
        val averageLuma: Float,
    ) {
        fun asText(): String =
            buildString {
                append("BITMAP")
                append(" width=").append(width)
                append(" height=").append(height)
                append(" sampled=").append(sampledPixels)
                append(" alphaCoverage=").append(String.format(Locale.US, "%.3f", alphaCoverage))
                append(" avgColor=").append(averageColorHex)
                append(" avgLuma=").append(String.format(Locale.US, "%.3f", averageLuma))
                append(" sampledBounds=")
                if (boundsLeft == null) {
                    append("empty")
                } else {
                    append("(")
                        .append(boundsLeft).append(",")
                        .append(boundsTop).append(")-")
                        .append("(").append(boundsRight).append(",")
                        .append(boundsBottom).append(")")
                }
            }
    }

    fun summarize(bitmap: Bitmap): Summary {
        require(!bitmap.isRecycled) { "Cannot inspect a recycled bitmap." }
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return Summary(width, height, 0, 0f, null, null, null, null, "#00000000", 0f)
        }

        val totalPixels = width.toLong() * height.toLong()
        val stride = max(1, ceil(sqrt(totalPixels.toDouble() / MAX_SAMPLED_PIXELS)).toInt())
        val row = IntArray(width)
        var sampled = 0
        var alphaHits = 0
        var sumA = 0L
        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var sumLuma = 0.0
        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        var y = 0
        while (y < height) {
            bitmap.getPixels(row, 0, width, 0, y, width, 1)
            var x = 0
            while (x < width) {
                val color = row[x]
                val a = (color ushr 24) and 0xFF
                val r = (color ushr 16) and 0xFF
                val g = (color ushr 8) and 0xFF
                val b = color and 0xFF
                sampled++
                alphaHits += if (a > 0) 1 else 0
                sumA += a
                sumR += r
                sumG += g
                sumB += b
                sumLuma += (0.2126 * r) + (0.7152 * g) + (0.0722 * b)
                if (a > 0) {
                    minX = minOf(minX, x)
                    minY = minOf(minY, y)
                    maxX = maxOf(maxX, x)
                    maxY = maxOf(maxY, y)
                }
                x += stride
            }
            y += stride
        }

        val sampleCount = sampled.coerceAtLeast(1)
        val avgA = averageByte(sumA, sampleCount)
        val avgR = averageByte(sumR, sampleCount)
        val avgG = averageByte(sumG, sampleCount)
        val avgB = averageByte(sumB, sampleCount)

        return Summary(
            width = width,
            height = height,
            sampledPixels = sampled,
            alphaCoverage = alphaHits.toFloat() / sampleCount,
            boundsLeft = minX.takeUnless { maxX < 0 },
            boundsTop = minY.takeUnless { maxX < 0 },
            boundsRight = maxX.takeUnless { maxX < 0 },
            boundsBottom = maxY.takeUnless { maxX < 0 },
            averageColorHex = "#%02X%02X%02X%02X".format(Locale.US, avgA, avgR, avgG, avgB),
            averageLuma = ((sumLuma / sampleCount) / 255.0).toFloat(),
        )
    }

    private fun averageByte(sum: Long, count: Int): Int =
        (sum.toDouble() / count).roundToInt().coerceIn(0, 255)
}
