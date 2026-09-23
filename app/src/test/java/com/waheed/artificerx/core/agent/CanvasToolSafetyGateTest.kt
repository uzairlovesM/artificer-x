package com.waheed.artificerx.core.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasToolSafetyGateTest {
    @Test
    fun drawPathClampsGeometryAndOpacity() {
        val call = ParsedToolCall.DrawPath(
            points = listOf(-10f, -4f, 99_999f, 99_999f),
            colorHex = "#FFFFFF",
            strokeWidthPx = 99_999f,
            opacity = 7f,
            brushType = null,
        )

        val outcome = CanvasToolSafetyGate.sanitize(call, canvasWidth = 1_000, canvasHeight = 800)

        assertTrue(outcome is CanvasToolSafetyGate.Outcome.Accepted)
        val safe = (outcome as CanvasToolSafetyGate.Outcome.Accepted).call as ParsedToolCall.DrawPath
        assertEquals(listOf(0f, 0f, 999f, 799f), safe.points)
        assertEquals(4_096f, safe.strokeWidthPx!!, 0f)
        assertEquals(1f, safe.opacity!!, 0f)
    }

    @Test
    fun nonFiniteGeometryIsRejected() {
        val call = ParsedToolCall.DrawCurve(
            startX = Float.NaN,
            startY = 1f,
            controlX = 2f,
            controlY = 3f,
            endX = 4f,
            endY = 5f,
            colorHex = "#FFFFFF",
            strokeWidthPx = 4f,
        )

        val outcome = CanvasToolSafetyGate.sanitize(call, 1_000, 800)

        assertTrue(outcome is CanvasToolSafetyGate.Outcome.Rejected)
    }

    @Test
    fun invalidBlendModeIsRejectedAndValidModeIsNormalized() {
        val invalid = CanvasToolSafetyGate.sanitize(
            ParsedToolCall.SetLayerProperty("layer", 2f, "not_real", true),
            1_000,
            800,
        )
        assertTrue(invalid is CanvasToolSafetyGate.Outcome.Rejected)

        val valid = CanvasToolSafetyGate.sanitize(
            ParsedToolCall.SetLayerProperty("layer", 2f, " MULTIPLY ", true),
            1_000,
            800,
        )
        assertTrue(valid is CanvasToolSafetyGate.Outcome.Accepted)
        val safe = (valid as CanvasToolSafetyGate.Outcome.Accepted).call as ParsedToolCall.SetLayerProperty
        assertEquals(1f, safe.opacity!!, 0f)
        assertEquals("multiply", safe.blendMode)
    }

    @Test
    fun oversizedResizeIsRejected() {
        val outcome = CanvasToolSafetyGate.sanitize(
            ParsedToolCall.ResizeCanvas(8_001, 1_000),
            2_000,
            2_000,
        )
        assertTrue(outcome is CanvasToolSafetyGate.Outcome.Rejected)
    }

    @Test
    fun selectionAndTransformStayInsideCanvasEnvelope() {
        val selection = CanvasToolSafetyGate.sanitize(
            ParsedToolCall.SetSelection(-50f, -20f, 9_000f, 9_000f),
            1_000,
            800,
        )
        assertTrue(selection is CanvasToolSafetyGate.Outcome.Accepted)
        val safeSelection = (selection as CanvasToolSafetyGate.Outcome.Accepted).call as ParsedToolCall.SetSelection
        assertEquals(0f, safeSelection.left, 0f)
        assertEquals(799f, safeSelection.bottom, 0f)

        val transform = CanvasToolSafetyGate.sanitize(
            ParsedToolCall.TransformLayer(
                dx = 99_999f,
                dy = -99_999f,
                scaleFactor = 100f,
                rotationDegrees = 1_000f,
                pivotX = 99_999f,
                pivotY = -2f,
            ),
            1_000,
            800,
        )
        assertTrue(transform is CanvasToolSafetyGate.Outcome.Accepted)
        val safeTransform = (transform as CanvasToolSafetyGate.Outcome.Accepted).call as ParsedToolCall.TransformLayer
        assertEquals(4_000f, safeTransform.dx, 0f)
        assertEquals(-3_200f, safeTransform.dy, 0f)
        assertEquals(20f, safeTransform.scaleFactor, 0f)
        assertEquals(360f, safeTransform.rotationDegrees, 0f)
        assertEquals(999f, safeTransform.pivotX!!, 0f)
        assertEquals(0f, safeTransform.pivotY!!, 0f)
    }
}
