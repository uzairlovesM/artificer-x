package com.waheed.artificerx.core.nativeops

import kotlin.math.ceil
import kotlin.math.sqrt

/** Converts Android Bitmap ARGB_8888 packed ints into explicit RGBA byte order for JNI. */
internal fun packArgb8888ToRgba(color: Int, output: ByteArray, offset: Int) {
    require(offset >= 0 && offset + 4 <= output.size) { "RGBA output offset is out of bounds" }
    output[offset] = (color and 0xFF).toByte()
    output[offset + 1] = ((color ushr 8) and 0xFF).toByte()
    output[offset + 2] = ((color ushr 16) and 0xFF).toByte()
    output[offset + 3] = ((color ushr 24) and 0xFF).toByte()
}

internal fun sampleStride(width: Int, height: Int, maxSampledPixels: Int): Int {
    require(width > 0 && height > 0) { "Bitmap dimensions must be positive" }
    require(maxSampledPixels > 0) { "Sample budget must be positive" }
    val totalPixels = width.toLong() * height.toLong()
    if (totalPixels <= maxSampledPixels.toLong()) return 1
    return ceil(sqrt(totalPixels.toDouble() / maxSampledPixels.toDouble()))
        .toInt()
        .coerceAtLeast(1)
}

internal fun sampledDimension(size: Int, stride: Int): Int {
    require(size > 0 && stride > 0)
    return (size + stride - 1) / stride
}
