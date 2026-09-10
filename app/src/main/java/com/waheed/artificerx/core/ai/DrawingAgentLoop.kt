package com.waheed.artificerx.core.ai

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Canvas
import com.waheed.artificerx.ai.drawing.BrushEngine
import com.waheed.artificerx.ai.ai.common.DrawingContext
import com.waheed.artificerx.ai.drawing.StrokePathCapability
import kotlinx.coroutines.CancellationException

/**
 * Small, dependency-light drawing agent loop. It applies a validated intent to the live
 * bitmap instead of referencing speculative pipeline/capability classes.
 */
class DrawingAgentLoop {
    suspend fun execute(intent: DrawingIntent): Result<Bitmap> {
        return try {
            val bitmap = intent.baseImage.copy(Bitmap.Config.ARGB_8888, true)
            if (intent.paths.isEmpty()) return Result.success(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = intent.size.coerceAtLeast(0.5f)
                color = intent.color
                alpha = (intent.opacity.coerceIn(0f, 1f) * 255f).toInt()
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            Canvas(bitmap).apply { intent.paths.forEach { drawPath(it, paint) } }
            Result.success(bitmap)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
