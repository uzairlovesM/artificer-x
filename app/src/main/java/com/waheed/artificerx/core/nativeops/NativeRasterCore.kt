package com.waheed.artificerx.core.nativeops

import android.graphics.Bitmap
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Native raster analysis boundary used by the artwork inspector and diagnostics. */
@Singleton
class NativeRasterCore @Inject constructor() {
    companion object {
        private const val MAX_NATIVE_SAMPLE_PIXELS = 1_048_576

        init {
            runCatching { System.loadLibrary("artificerx_native") }
        }

        @JvmStatic
        private external fun nativeAnalyzeRgba(rgba: ByteArray, width: Int, height: Int): String
    }

    fun analyze(bitmap: Bitmap): String {
        require(!bitmap.isRecycled) { "Cannot analyze a recycled bitmap" }
        require(bitmap.width > 0 && bitmap.height > 0) { "Bitmap dimensions must be positive" }

        val source =
            if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                bitmap
            }
        requireNotNull(source) { "Hardware bitmap cannot be converted to ARGB_8888" }

        try {
            val stride = sampleStride(source.width, source.height, MAX_NATIVE_SAMPLE_PIXELS)
            val sampleWidth = sampledDimension(source.width, stride)
            val sampleHeight = sampledDimension(source.height, stride)
            val rgba = ByteArray(sampleWidth * sampleHeight * 4)
            val row = IntArray(source.width)
            var cursor = 0
            var y = 0
            while (y < source.height) {
                source.getPixels(row, 0, source.width, 0, y, source.width, 1)
                var x = 0
                while (x < source.width) {
                    packArgb8888ToRgba(row[x], rgba, cursor)
                    cursor += 4
                    x += stride
                }
                y += stride
            }

            val nativeStats =
                runCatching { nativeAnalyzeRgba(rgba, sampleWidth, sampleHeight) }
                    .getOrNull()
            val engine = if (nativeStats != null) "native" else "kotlin-fallback"
            val stats = nativeStats ?: analyzeRgbaInKotlin(rgba, sampleWidth, sampleHeight)
            return addMetadata(
                stats = stats,
                sourceWidth = bitmap.width,
                sourceHeight = bitmap.height,
                sampleStride = stride,
                engine = engine,
            )
        } finally {
            if (source !== bitmap) source.recycle()
        }
    }

    private fun analyzeRgbaInKotlin(rgba: ByteArray, width: Int, height: Int): String {
        val pixelCount = width.toLong() * height.toLong()
        if (pixelCount <= 0L || rgba.size < pixelCount * 4L) {
            return "{\"error\":\"invalid_buffer\"}"
        }

        var sumR = 0L
        var sumG = 0L
        var sumB = 0L
        var sumA = 0L
        var sumLuma = 0.0
        var opaque = 0L
        var i = 0
        while (i < pixelCount.toInt()) {
            val offset = i * 4
            val r = rgba[offset].toInt() and 0xFF
            val g = rgba[offset + 1].toInt() and 0xFF
            val b = rgba[offset + 2].toInt() and 0xFF
            val a = rgba[offset + 3].toInt() and 0xFF
            sumR += r
            sumG += g
            sumB += b
            sumA += a
            sumLuma += (0.2126 * r) + (0.7152 * g) + (0.0722 * b)
            if (a == 255) opaque++
            i++
        }

        val invPixels = 1.0 / pixelCount.toDouble()
        return String.format(
            Locale.US,
            "{\"avg_rgba\":[%.3f,%.3f,%.3f,%.3f],\"avg_luminance\":%.5f,\"opaque_coverage\":%.5f,\"pixels\":%d}",
            sumR * invPixels,
            sumG * invPixels,
            sumB * invPixels,
            sumA * invPixels,
            sumLuma * invPixels,
            opaque * invPixels,
            pixelCount,
        )
    }

    private fun addMetadata(
        stats: String,
        sourceWidth: Int,
        sourceHeight: Int,
        sampleStride: Int,
        engine: String,
    ): String {
        if (!stats.endsWith("}")) return stats
        return stats.removeSuffix("}") +
            ",\"source_width\":$sourceWidth," +
            "\"source_height\":$sourceHeight," +
            "\"sample_stride\":$sampleStride," +
            "\"engine\":\"$engine\"}"
    }
}
