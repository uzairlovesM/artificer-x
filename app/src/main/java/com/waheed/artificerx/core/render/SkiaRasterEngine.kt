package com.waheed.artificerx.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Surface
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Explicit Skia-backed raster path for large/batched operations.
 *
 * This is intentionally an offscreen backend. Android/Compose UI stays on its
 * native rendering surface, while large AI-generated paths and exports can use
 * Skia's mature rasterizer without introducing a second on-screen scene graph.
 */
@Singleton
class SkiaRasterEngine @Inject constructor(
    private val policy: GraphicsBackendPolicy,
    private val memoryBudget: RenderMemoryBudget,
    private val telemetry: RenderTelemetry,
) {
    private var availability: Boolean? = null

    fun isAvailable(): Boolean {
        availability?.let { return it }
        val result = runCatching {
            val surface = Surface.makeRasterN32Premul(1, 1)
            try {
                surface.canvas.clear(Color.TRANSPARENT)
            } finally {
                surface.close()
            }
            true
        }.getOrElse { false }
        availability = result
        return result
    }

    /**
     * Draws one heavy polyline through Skia and composites the resulting PNG
     * back into the target Android Canvas. This encoding round-trip is used
     * only above the batch threshold, where JNI/native raster work outweighs
     * the transfer cost, and it always falls back to the caller's Canvas path.
     */
    fun tryDrawLargeStroke(
        targetCanvas: Canvas,
        points: List<Float>,
        colorArgb: Int,
        strokeWidthPx: Float,
        opacity: Float,
    ): Boolean {
        if (!isAvailable()) return false
        if (points.size < 8 || points.size % 2 != 0) return false
        val xs = points.filterIndexed { index, _ -> index % 2 == 0 }
        val ys = points.filterIndexed { index, _ -> index % 2 == 1 }
        val minX = xs.minOrNull() ?: 0f
        val minY = ys.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 1f
        val maxY = ys.maxOrNull() ?: 1f
        val padding = strokeWidthPx.coerceAtLeast(1f) + 2f
        val width = max(1, (maxX - minX + padding * 2f).toInt())
        val height = max(1, (maxY - minY + padding * 2f).toInt())
        if (policy.chooseOffscreenBackend(width, height, interactive = false, complexBlend = false) != OffscreenBackend.SKIA) {
            return false
        }

        val translatedPoints = points.mapIndexed { index, value ->
            value - if (index % 2 == 0) minX - padding else minY - padding
        }

        return runCatching {
            telemetry.section("artificerx-skia-stroke") {
            val cacheKey = buildString(96) {
                append(colorArgb).append(':')
                append(strokeWidthPx).append(':')
                append(opacity).append(':')
                append(width).append('x').append(height).append(':')
                append(translatedPoints.hashCode())
            }
            val cached = memoryBudget.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                targetCanvas.drawBitmap(cached, minX - padding, minY - padding, null)
                return true
            }

            val skiaPng = renderStrokePng(translatedPoints, colorArgb, strokeWidthPx, opacity, width, height)
            val decoded = android.graphics.BitmapFactory.decodeByteArray(skiaPng, 0, skiaPng.size)
                ?: return false
            targetCanvas.drawBitmap(decoded, minX - padding, minY - padding, null)
            if (decoded.allocationByteCount.toLong() <= memoryBudget.maxBytes) {
                memoryBudget.put(cacheKey, decoded)
            } else {
                decoded.recycle()
            }
            true
            }
        }.getOrElse { false }
    }

    fun renderStrokePng(
        points: List<Float>,
        colorArgb: Int,
        strokeWidthPx: Float,
        opacity: Float,
        width: Int,
        height: Int,
    ): ByteArray {
        require(points.size >= 4 && points.size % 2 == 0)
        require(width > 0 && height > 0)
        val surface = Surface.makeRasterN32Premul(width, height)
        try {
            surface.canvas.clear(Color.TRANSPARENT)
            val paint = Paint().apply {
                color = colorArgb
                this.alpha = (opacity.coerceIn(0f, 1f) * 255f).toInt()
                isAntiAlias = true
                strokeWidth = strokeWidthPx.coerceAtLeast(0.5f)
            }
            var index = 0
            while (index + 3 < points.size) {
                surface.canvas.drawLine(
                    points[index],
                    points[index + 1],
                    points[index + 2],
                    points[index + 3],
                    paint,
                )
                index += 2
            }
            val image = surface.makeImageSnapshot()
            try {
                val encoded = image.encodeToData(EncodedImageFormat.PNG, 100, 1)
                    ?: error("Skia PNG encode failed")
                return encoded.bytes
            } finally {
                image.close()
            }
        } finally {
            surface.close()
        }
    }

    /** Rasterizes an already-encoded image through Skia and returns PNG again. */
    fun roundTripPng(sourcePng: ByteArray, width: Int, height: Int): ByteArray = runCatching {
        val source = Image.makeFromEncoded(sourcePng)
        try {
            val surface = Surface.makeRasterN32Premul(width.coerceAtLeast(1), height.coerceAtLeast(1))
            try {
                surface.canvas.clear(Color.TRANSPARENT)
                surface.canvas.drawImage(source, 0f, 0f)
                val out = surface.makeImageSnapshot()
                try {
                    return@runCatching out.encodeToData(EncodedImageFormat.PNG, 100, 1)?.bytes
                        ?: sourcePng
                } finally {
                    out.close()
                }
            } finally {
                surface.close()
            }
        } finally {
            source.close()
        }
    }.getOrElse { sourcePng }
}
