package com.waheed.artificerx.core.drawing
import org.junit.Assert.*
import org.junit.Test

class StrokeGeometryTest {
    @Test fun computesLength() {
        val points = listOf(StrokePoint(0f,0f), StrokePoint(3f,4f), StrokePoint(6f,4f))
        assertEquals(8f, StrokeGeometry.length(points), .001f)
    }
    @Test fun computesBounds() {
        val bounds = StrokeGeometry.bounds(listOf(StrokePoint(4f,8f), StrokePoint(-2f,3f)))!!
        assertEquals(-2f,bounds.left,.001f); assertEquals(8f,bounds.top,.001f)
    }
}
