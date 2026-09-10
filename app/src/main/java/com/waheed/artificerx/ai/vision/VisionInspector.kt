package com.waheed.artificerx.ai.vision

import android.graphics.Bitmap
import kotlin.math.abs

class VisionInspector {
    fun inspect(frame: VisionFrame): VisionObservation {
        val bitmap = frame.bitmap
        val brightness = estimateBrightness(bitmap)
        val edgeDensity = estimateEdgeDensity(bitmap)
        val palette = estimatePalette(bitmap)
        val issues = buildList {
            if (edgeDensity < 0.015f) add(VisionIssue("LOW_STRUCTURE", Severity.WARNING, "Canvas has unusually little structural edge information"))
            if (brightness < 0.08f) add(VisionIssue("DARK_FRAME", Severity.INFO, "Frame is predominantly dark"))
            if (brightness > 0.92f) add(VisionIssue("BRIGHT_FRAME", Severity.INFO, "Frame is predominantly bright"))
            if (bitmap.width < 256 || bitmap.height < 256) add(VisionIssue("LOW_RESOLUTION", Severity.WARNING, "Inspection resolution is below 256px on one axis"))
        }
        return VisionObservation(
            sceneType = classifyScene(edgeDensity, brightness, palette),
            objects = emptyList(),
            spatialRelations = emptyList(),
            palette = palette,
            compositionScore = ((edgeDensity * 6f) + colorSeparation(palette) * 0.4f).coerceIn(0f, 1f),
            perspectiveScore = (edgeDensity * 2.5f).coerceIn(0f, 1f),
            completenessScore = (1f - issues.count { it.severity == Severity.BLOCKING } * .25f).coerceIn(0f, 1f),
            issues = issues,
        )
    }

    private fun classifyScene(edges: Float, brightness: Float, palette: List<String>): String = when {
        edges < 0.015f && brightness < 0.25f -> "dark-low-structure"
        edges < 0.015f -> "soft-minimal"
        palette.size <= 2 -> "graphic-or-flat"
        edges > 0.16f -> "high-detail"
        else -> "illustration"
    }

    private fun estimateBrightness(bitmap: Bitmap): Float {
        val sx = (bitmap.width / 32).coerceAtLeast(1)
        val sy = (bitmap.height / 32).coerceAtLeast(1)
        var total = 0L
        var count = 0
        for (y in 0 until bitmap.height step sy) for (x in 0 until bitmap.width step sx) {
            val c = bitmap.getPixel(x, y)
            total += ((c shr 16 and 255) + (c shr 8 and 255) + (c and 255)) / 3
            count++
        }
        return if (count == 0) 0f else total.toFloat() / count / 255f
    }

    private fun estimateEdgeDensity(bitmap: Bitmap): Float {
        if (bitmap.width < 2 || bitmap.height < 2) return 0f
        var edges = 0
        var samples = 0
        val sx = (bitmap.width / 64).coerceAtLeast(1)
        val sy = (bitmap.height / 64).coerceAtLeast(1)
        for (y in 0 until bitmap.height - sy step sy) for (x in 0 until bitmap.width - sx step sx) {
            val a = bitmap.getPixel(x, y)
            val b = bitmap.getPixel(x + sx, y + sy)
            val la = luminance(a)
            val lb = luminance(b)
            if (abs(la - lb) > 40) edges++
            samples++
        }
        return if (samples == 0) 0f else edges.toFloat() / samples
    }

    private fun estimatePalette(bitmap: Bitmap): List<String> {
        val bins = LinkedHashMap<Int, Int>()
        val sx = (bitmap.width / 48).coerceAtLeast(1)
        val sy = (bitmap.height / 48).coerceAtLeast(1)
        for (y in 0 until bitmap.height step sy) for (x in 0 until bitmap.width step sx) {
            val c = bitmap.getPixel(x, y)
            val r = (c shr 16 and 255) / 32
            val g = (c shr 8 and 255) / 32
            val b = (c and 255) / 32
            val key = (r shl 8) or (g shl 4) or b
            bins[key] = (bins[key] ?: 0) + 1
        }
        return bins.entries.sortedByDescending { it.value }.take(6).map { entry ->
            val r = ((entry.key shr 8) and 15) * 17
            val g = ((entry.key shr 4) and 15) * 17
            val b = (entry.key and 15) * 17
            "#%02X%02X%02X".format(r, g, b)
        }.distinct()
    }

    private fun colorSeparation(palette: List<String>): Float = (palette.size / 6f).coerceIn(0f, 1f)
    private fun luminance(c: Int): Int = ((54 * (c shr 16 and 255)) + (183 * (c shr 8 and 255)) + (19 * (c and 255))) / 256
}
