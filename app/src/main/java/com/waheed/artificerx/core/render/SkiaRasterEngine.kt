package com.waheed.artificerx.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint as AndroidPaint
import android.graphics.Path
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Android-Canvas-backed raster engine for large/batched operations.
 * Previously used org.jetbrains.skiko (skiko-android) which is not
 * published to Maven Central for Android targets. Replaced with
 * android.graphics.* equivalents — same API surface, zero extra dep.
 */
@Singleton
class SkiaRasterEngine @Inject constructor(
    private val policy: GraphicsBackendPolicy,
    private val memoryBudget: RenderMemoryBudget,
    private val telemetry: RenderTelemetry,
) {
    fun isAvailable(): Boolean = true

    fun tryDrawLargeStroke(
        targetCanvas: Canvas,
        points: List<Float>,
        colorArgb: Int,
        strokeWidthPx: Float,
        opacity: Float,
    ): Boolean {
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
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT)
        val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            color = colorArgb
            alpha = (opacity.coerceIn(0f, 1f) * 255f).toInt()
            style = AndroidPaint.Style.STROKE
            strokeCap = AndroidPaint.Cap.ROUND
            strokeJoin = AndroidPaint.Join.ROUND
            this.strokeWidth = strokeWidthPx.coerceAtLeast(0.5f)
        }
        val path = Path()
        var index = 0
        path.moveTo(points[0], points[1])
        index = 2
        while (index + 1 < points.size) {
            path.lineTo(points[index], points[index + 1])
            index += 2
        }
        canvas.drawPath(path, paint)
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        return out.toByteArray()
    }

    fun roundTripPng(sourcePng: ByteArray, width: Int, height: Int): ByteArray = runCatching {
        val source = android.graphics.BitmapFactory.decodeByteArray(sourcePng, 0, sourcePng.size)
            ?: return sourcePng
        val bmp = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawBitmap(source, 0f, 0f, null)
        source.recycle()
        val out = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        bmp.recycle()
        out.toByteArray()
    }.getOrElse { sourcePng }
}
