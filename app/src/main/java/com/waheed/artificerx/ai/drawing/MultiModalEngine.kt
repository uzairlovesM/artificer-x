package com.waheed.artificerx.ai.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.waheed.artificerx.ai.ai.common.DrawingCapability
import com.waheed.artificerx.ai.ai.common.DrawingContext
import com.waheed.artificerx.ai.ai.common.DrawingResult
import com.waheed.artificerx.ai.vision.VisionInspector

open class MultiModalEngine(
    private val inspector: VisionInspector = VisionInspector(),
) : DrawingCapability {
    override suspend fun process(context: DrawingContext): DrawingResult {
        val observation = inspector.inspect(com.waheed.artificerx.ai.vision.VisionFrame(context.baseImage, com.waheed.artificerx.ai.vision.VisionSource.CANVAS))
        val output = context.baseImage.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val quality = observation.compositionScore.coerceIn(0f, 1f)
        if (quality < 0.25f) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(180, 255, 180, 60)
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Low-structure canvas: improve silhouette or contrast", 24f, 42f, paint)
        }
        val note = if (context.instruction.isBlank()) "visual inspection complete" else "instruction evaluated: ${context.instruction.take(96)}"
        return DrawingResult(output, notes = listOf(note, "scene=${observation.sceneType}", "objects=${observation.objects.size}"), confidence = quality)
    }
}
