package com.waheed.artificerx.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) {
    data class ImageStats(
        val width: Int,
        val height: Int,
        val hasAlpha: Boolean,
        val meanLuma: Double,
        val opaqueRatio: Double,
    )

    suspend fun processImage(imagePath: String): Bitmap? = withContext(Dispatchers.Default) {
        val source = BitmapFactory.decodeFile(imagePath) ?: return@withContext null
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, null, Rect(0, 0, result.width, result.height), paint)
        if (result !== source) source.recycle()
        result
    }

    suspend fun getImageStats(imagePath: String): ImageStats = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: throw IllegalArgumentException("Unable to decode image: $imagePath")
        try {
            var luminance = 0.0
            var opaque = 0L
            val pixels = IntArray((bitmap.width * bitmap.height).coerceAtMost(1_000_000))
            val sampleW = minOf(bitmap.width, maxOf(1, kotlin.math.sqrt(pixels.size.toDouble()).toInt()))
            val sampleH = minOf(bitmap.height, maxOf(1, pixels.size / sampleW))
            bitmap.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
            val n = sampleW * sampleH
            for (i in 0 until n) {
                val c = pixels[i]
                val r = (c ushr 16) and 0xff
                val g = (c ushr 8) and 0xff
                val b = c and 0xff
                luminance += 0.2126 * r + 0.7152 * g + 0.0722 * b
                if ((c ushr 24) and 0xff >= 250) opaque++
            }
            ImageStats(bitmap.width, bitmap.height, bitmap.hasAlpha(), if (n == 0) 0.0 else luminance / n, if (n == 0) 0.0 else opaque.toDouble() / n)
        } finally {
            bitmap.recycle()
        }
    }

    fun releaseResources() = Unit

    fun cacheDirectory(): File = File(context.cacheDir, "native")
}
