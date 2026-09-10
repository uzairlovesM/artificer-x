package com.waheed.artificerx.core.art

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdvancedDrawingEnginesTest {
    @Test
    fun strokeProcessor_keepsEndpoints_andProducesValidGeometry() {
        val input = listOf(0f, 0f, 2f, 1f, 4f, 3f, 7f, 5f, 11f, 6f, 16f, 8f)
        val result = AdvancedStrokeProcessor().process(input, smoothing = 0.55f, spacing = 0.22f)
        assertEquals(input.first(), result.points.first())
        assertEquals(input[input.lastIndex - 1], result.points[result.points.lastIndex - 1])
        assertEquals(input.last(), result.points.last())
        assertTrue(result.outputPointCount >= 2)
        assertTrue(result.qualityScore in 0f..1f)
    }

    @Test
    fun guideEngine_creates_expected_grid_and_safeArea() {
        val engine = CanvasGuideEngine()
        assertEquals(14, engine.grid(1000, 1000, 8).size)
        assertEquals(4, engine.safeArea(1000, 1000).size)
    }
}
