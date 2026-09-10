package com.waheed.artificerx.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream

/** Optional converter used only by legacy callers; the canonical project DB stores file paths. */
class ImageBitmapRoom {
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap?): ByteArray? = bitmap?.let(::bitmapToBytes)

    @TypeConverter
    fun toBitmap(bytes: ByteArray?): Bitmap? = bytes?.let(::bytesToBitmap)

    private fun bitmapToBytes(bitmap: Bitmap): ByteArray = ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.toByteArray()
    }

    private fun bytesToBitmap(bytes: ByteArray): Bitmap? = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
