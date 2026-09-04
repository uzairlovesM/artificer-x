package com.waheed.artificerx.core.nativeops

import android.graphics.Bitmap
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton

/** Native raster analysis boundary used by the artwork inspector and diagnostics. */
@Singleton
class NativeRasterCore @Inject constructor() {
    companion object {
        init { System.loadLibrary("artificerx_native") }
        @JvmStatic private external fun nativeAnalyzeRgba(rgba: ByteArray, width: Int, height: Int): String
    }

    fun analyze(bitmap: Bitmap): String {
        require(!bitmap.isRecycled) { "Cannot analyze a recycled bitmap" }
        val safe = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        requireNotNull(safe) { "Bitmap cannot be converted to ARGB_8888" }
        val buffer = ByteBuffer.allocate(safe.byteCount)
        safe.copyPixelsToBuffer(buffer)
        if (safe !== bitmap) safe.recycle()
        return nativeAnalyzeRgba(buffer.array(), bitmap.width, bitmap.height)
    }
}
