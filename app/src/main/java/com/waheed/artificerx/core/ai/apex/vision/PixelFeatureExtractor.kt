package com.waheed.artificerx.core.ai.apex.vision

import kotlin.math.sqrt

data class PixelFeatures(val luminanceMean: Double, val luminanceStdDev: Double, val alphaCoverage: Double, val edgeEnergy: Double, val dominantArgb: Int)
class PixelFeatureExtractor {
    fun extract(pixels: IntArray): PixelFeatures {
        if (pixels.isEmpty()) return PixelFeatures(0.0, 0.0, 0.0, 0.0, 0)
        var sum = 0.0; var sumSq = 0.0; var alpha = 0L; var edges = 0.0
        val bins = IntArray(27)
        for (i in pixels.indices) {
            val p = pixels[i]; val r = p ushr 16 and 255; val g = p ushr 8 and 255; val b = p and 255; val a = p ushr 24 and 255
            val y = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            sum += y; sumSq += y * y; if (a > 0) alpha++
            val bucket = (r / 64) * 9 + (g / 64) * 3 + (b / 64)
            bins[bucket.coerceIn(0, 26)]++
            if (i > 0) {
                val prev = pixels[i - 1]
                val pr = prev ushr 16 and 255; val pg = prev ushr 8 and 255; val pb = prev and 255
                edges += sqrt(((r - pr) * (r - pr) + (g - pg) * (g - pg) + (b - pb) * (b - pb)).toDouble()) / 441.6729559
            }
        }
        val mean = sum / pixels.size; val variance = (sumSq / pixels.size - mean * mean).coerceAtLeast(0.0)
        val idx = bins.indices.maxByOrNull { bins[it] } ?: 0
        val br = (idx / 9) * 64 + 32; val bg = ((idx / 3) % 3) * 64 + 32; val bb = (idx % 3) * 64 + 32
        return PixelFeatures(mean, sqrt(variance), alpha.toDouble() / pixels.size, edges / pixels.size.coerceAtLeast(1), 0xff000000.toInt() or (br shl 16) or (bg shl 8) or bb)
    }
}
