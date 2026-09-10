package com.waheed.artificerx.core.drawing.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokePipelineTest {
    @Test
    fun pipelineKeepsEndpointsAndProducesSegmentWeights() {
        val points = buildList {
            for (i in 0..40) {
                add(i.toFloat() * 4f)
                add((i % 3).toFloat())
            }
        }
        val result = StrokePipeline().process(
            points,
            pressureWeights = List(41) { 0.7f },
            config = StrokePipeline.Config(.25f, .15f, .5f),
        )
        assertTrue(result.outputPointCount >= 2)
        assertEquals(points.first(), result.points.first())
        assertEquals(points.last(), result.points.last())
        assertEquals(result.outputPointCount - 1, result.segmentWeights.size)
        assertTrue(result.quality in 0f..1f)
    }

    @Test
    fun viewportZoomKeepsAnchorStable() {
        val viewport = ViewportTransform(1000f, 1000f, 1200f, 900f)
        val zoomed = viewport.zoomAround(600f, 450f, 2f)
        val originalCanvas = viewport.screenToCanvas(600f, 450f)
        val mappedBack = zoomed.canvasToScreen(originalCanvas.first, originalCanvas.second)
        assertTrue(kotlin.math.abs(mappedBack.first - 600f) < 0.01f)
        assertTrue(kotlin.math.abs(mappedBack.second - 450f) < 0.01f)
    }
}
