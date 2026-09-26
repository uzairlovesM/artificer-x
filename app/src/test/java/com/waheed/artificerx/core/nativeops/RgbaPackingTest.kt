package com.waheed.artificerx.core.nativeops

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RgbaPackingTest {
    @Test
    fun argb8888IsPackedAsExplicitRgba() {
        val output = ByteArray(4)
        packArgb8888ToRgba(0x44332211, output, 0)
        assertArrayEquals(byteArrayOf(0x11, 0x22, 0x33, 0x44), output)
    }

    @Test
    fun samplingKeepsLargeBitmapAllocationBounded() {
        val stride = sampleStride(8000, 8000, 1_048_576)
        val width = sampledDimension(8000, stride)
        val height = sampledDimension(8000, stride)
        assertTrue(stride > 1)
        assertTrue(width * height <= 1_048_576 + width + height)
    }

    @Test
    fun smallBitmapKeepsExactSampling() {
        assertEquals(1, sampleStride(1000, 1000, 1_000_000))
    }
}
