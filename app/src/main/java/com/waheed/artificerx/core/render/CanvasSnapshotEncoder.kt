package com.waheed.artificerx.core.render

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 156 vision-feedback loop: encodes the composited canvas as a
 * base64 PNG data-URL payload the Reasoning Brain can attach as an
 * image_url content part in its next request, letting it literally see
 * what it just drew. Downscales before encoding — full-resolution
 * canvas snapshots would blow past most free-tier providers' payload
 * size limits and burn tokens for no perception benefit at typical
 * mobile screen viewing sizes.
 */
@Singleton
class CanvasSnapshotEncoder
    @Inject
    constructor() {
        fun encodeForVisionFeedback(
            bitmap: Bitmap,
            maxDimensionPx: Int = 768,
        ): String {
            val scaled = downscaleIfNeeded(bitmap, maxDimensionPx)
            val outputStream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            val bytes = outputStream.toByteArray()
            if (scaled !== bitmap) scaled.recycle()
            return Base64.encodeToString(bytes, Base64.NO_WRAP)
        }

        private fun downscaleIfNeeded(
            bitmap: Bitmap,
            maxDimensionPx: Int,
        ): Bitmap {
            val largestSide = maxOf(bitmap.width, bitmap.height)
            if (largestSide <= maxDimensionPx) return bitmap
            val scale = maxDimensionPx.toFloat() / largestSide
            val newWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val newHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        }
    }
