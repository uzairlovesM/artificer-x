package com.waheed.artificerx.ai.vision

import android.graphics.Bitmap

class VisionInspector {
    fun inspect(frame: VisionFrame): VisionObservation {
        val bitmap = frame.bitmap
        val edgeDensity = estimateEdgeDensity(bitmap)
        val brightness = estimateBrightness(bitmap)
        val issues = buildList {
            if (edgeDensity < 0.015f) add(VisionIssue("LOW_STRUCTURE", Severity.WARNING, "Canvas has unusually little structural edge information"))
            if (brightness < 0.08f) add(VisionIssue("DARK_FRAME", Severity.INFO, "Frame is predominantly dark"))
            if (bitmap.width < 256 || bitmap.height < 256) add(VisionIssue("LOW_RESOLUTION", Severity.WARNING, "Inspection resolution is below 256px on one axis"))
        }
        return VisionObservation(
            sceneType = "unknown",
            objects = emptyList(),
            spatialRelations = emptyList(),
            palette = listOf("#000000", "#FFFFFF"),
            compositionScore = (edgeDensity * 8f).coerceIn(0f, 1f),
            perspectiveScore = 0f,
            completenessScore = (1f - issues.count { it.severity == Severity.BLOCKING } * .25f).coerceIn(0f, 1f),
            issues = issues
        )
    }

    private fun estimateBrightness(bitmap: Bitmap): Float {
        val stepX = (bitmap.width / 32).coerceAtLeast(1)
        val stepY = (bitmap.height / 32).coerceAtLeast(1)
        var total = 0L
        var count = 0
        for (y in 0 until bitmap.height step stepY) for (x in 0 until bitmap.width step stepX) {
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
            val da = (a shr 16 and 255) + (a shr 8 and 255) + (a and 255)
            val db = (b shr 16 and 255) + (b shr 8 and 255) + (b and 255)
            if (kotlin.math.abs(da - db) > 90) edges++
            samples++
        }
        return if (samples == 0) 0f else edges.toFloat() / samples
    }
}
