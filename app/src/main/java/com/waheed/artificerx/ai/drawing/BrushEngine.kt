package com.waheed.artificerx.ai.drawing

import android.content.Context
import android.graphics.*
import android.util.Log
import com.waheed.artificerx.core.runtime.Callback
import kotlinx.coroutines.*
import java.util.*

class BrushEngine(private val context: Context) : ErrorHandler by MultiModalErrorAggregator() {
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val brushRegistry = mutableMapOf<String, BrushData>()

    // Enhanced error handling for all brush operations
    @Throws(MultipleModelFailureException::class)
    suspend fun processBrushOperations(
        context: DrawingContext,
        baseImage: Bitmap,
        operations: List<((BrushEngine) -> Unit)>
    ): DrawingResult = try {
        val errors = mutableListOf<Throwable>()

        // Execute all operations with error collection
        for (op in operations) {
            try {
                op(this)
            } catch (e: Exception) {
                errors += e // Collect all errors
            }
        }

        // Process aggregated errors if any occurred
        if (errors.isNotEmpty()) {
            throw aggregateErrors(*errors.toTypedArray())
        }

        // Return successful result after all operations
        DrawingResult(baseImage)
    } catch (e: MultipleModelFailureException) {
        throw e // Pass aggregated errors up the chain
    }

    data class BrushData(
        val brushType: BrushType,
        val color: Int,
        val size: Float,
        val texture: Bitmap? = null,
        val opacity: Float = 1.0f
    )

    enum class BrushType(val description: String) {
        SOFT("Soft edge brush"),
        HARD("Hard edge brush"),
        AIRBRUSH("Airbrush for smooth gradients"),
        TEXTURED("Brush with texture"),
        ERASER("Erase brush")
    }

    fun registerBrush(brush: BrushData) {
        registerBrushAndReturnId(brush)
    }

    fun registerBrushAndReturnId(brush: BrushData): String {
        val brushId = "${brush.brushType.name}_${UUID.randomUUID()}"
        brushRegistry[brushId] = brush
        DebugLogger.d("BrushEngine", "Registered brush: $brushId with ${brush.brushType.description}")
        return brushId
    }

    suspend fun applyBrush(strokePath: Path, brushId: String, baseImage: Bitmap) = coroutineScope {
        val brush = brushRegistry[brushId] ?: throw IllegalArgumentException("Brush not found: $brushId")
        DebugLogger.d("BrushEngine", "Applying brush: ${brush.brushType} with id: $brushId")

        val paint = createPaintForBrush(brush, strokePath)
        val canvas = Canvas(baseImage)
        canvas.drawPath(strokePath, paint)
        DebugLogger.d("BrushEngine", "Finished applying brush: $brushId")
        baseImage
    }

    private fun createPaintForBrush(brush: BrushData, path: Path): Paint {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = brush.size
            color = brush.color
            alpha = (brush.opacity * 255).toInt()
        }

        when (brush.brushType) {
            BrushType.SOFT -> DashPathEffect(floatArrayOf(brush.size, brush.size * 2), 0f).also { paint.pathEffect = it }
            BrushType.HARD -> null
            BrushType.AIRBRUSH -> {
                val blur = BlurMaskFilter(brush.size * 1.5f, BlurMaskFilter.Blur.SOLID)
                paint.maskFilter = blur
            }
            BrushType.TEXTURED -> {
                val textureShader = BitmapShader(brush.texture, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                paint.shader = textureShader
                // Advanced texture emboss effect
                paint.maskFilter = EmbossMaskFilter(floatArrayOf(1.0f, 1.0f, 90f), 0.8f, brush.size * 2, 12f)
            }
            BrushType.ERASER -> EraserMask(brush.size, brush.size).also { paint.xfermode = it }
        }
        return paint
    }

    suspend fun applyTextureBrush(brushId: String, baseImage: Bitmap, texturePath: String) = coroutineScope {
        val texture = BitmapFactory.decodeFile(texturePath)
            ?: throw IllegalArgumentException("Texture not found: $texturePath")
        val brush = brushRegistry[brushId]?.copy(texture = texture)
            ?: throw IllegalArgumentException("Brush not found: $brushId")
        val shader = BitmapShader(texture, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = brush.size
            color = brush.color
            alpha = (brush.opacity.coerceIn(0f, 1f) * 255f).toInt()
            this.shader = shader
        }
        val canvas = Canvas(baseImage)
        canvas.drawCircle(texture.width / 2f, texture.height / 2f, maxOf(1f, brush.size / 2f), paint)
        baseImage
    }

    fun createPaintForPath(path: Path, brush: BrushData): Paint = createPaintForBrush(brush, path)

    fun unregisterBrush(brushId: String) {
        brushRegistry.remove(brushId)
        DebugLogger.d("BrushEngine", "Unregistered brush: $brushId")
    }

    fun clearBrushRegistry() {
        brushRegistry.clear()
        DebugLogger.d("BrushEngine", "Cleared all registered brushes")
    }

    fun getBrushCount(): Int = brushRegistry.size

    // Advanced pressure-aware brush adjustment
    private class PressureManager {
        fun adjustSize(baseSize: Float): Float = baseSize * (0.5f..2.0f).random() // Simulated pressure effect
    }

    // Advanced shape generators for complex brush shapes
    private object PathsUtils {
        fun makeTexturedPath(texture: Bitmap): Path {
            val path = Path()
            path.addCircle(texture.width / 2f, texture.height / 2f, texture.width / 4f, Path.Direction.CW)
            return path
        }
    }
}
