package com.waheed.artificerx.ai.drawing

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.content.Context
import com.waheed.artificerx.core.runtime.Callback
import java.io.File
import java.util.UUID

/** Bitmap-layer manager used by drawing agents and import/export helpers. */
class LayerManager(private val context: Context) {
    private val layerStack = mutableListOf<LayerData>()

    data class LayerData(
        val layerId: String,
        var layerName: String,
        val bitmap: Bitmap,
        var order: Int,
        var visible: Boolean = true,
        var opacity: Int = 255,
        var blendMode: BlendMode = BlendMode.SRC_OVER,
    )

    enum class BlendMode {
        SRC_OVER, MULTIPLY, SCREEN, OVERLAY, DARKEN, LIGHTEN,
    }

    val currentLayer: LayerData?
        get() = layerStack.sortedBy { it.order }.lastOrNull { it.visible }

    fun createNewLayer(name: String, templateImage: Bitmap? = null): LayerData {
        val id = "layer_${UUID.randomUUID()}"
        val order = (layerStack.maxOfOrNull { it.order } ?: -1) + 1
        val bitmap = templateImage?.copy(Bitmap.Config.ARGB_8888, true)
            ?: Bitmap.createBitmap(1024, 1024, Bitmap.Config.ARGB_8888)
        return LayerData(id, name.ifBlank { "Layer ${order + 1}" }, bitmap, order).also { layerStack += it }
    }

    fun drawOnLayer(
        layerId: String,
        paths: List<Path>,
        color: Int,
        size: Float,
        brushType: BrushEngine.BrushType,
        callback: Callback<Bitmap>,
    ) {
        val layer = layerStack.firstOrNull { it.layerId == layerId }
        if (layer == null) {
            callback.onFailure(IllegalArgumentException("Layer not found: $layerId"))
            return
        }
        runCatching {
            val paint = createPaint(brushType, color, size)
            Canvas(layer.bitmap).apply { paths.forEach { drawPath(it, paint) } }
            callback.onSuccess(layer.bitmap)
        }.onFailure(callback::onFailure)
    }

    fun mergeLayerInto(layerId: String, targetCanvas: Canvas) {
        val layer = layerStack.firstOrNull { it.layerId == layerId }
            ?: throw IllegalArgumentException("Layer not found: $layerId")
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = layer.opacity.coerceIn(0, 255)
            xfermode = PorterDuffXfermode(toPorterDuff(layer.blendMode))
        }
        if (layer.visible) targetCanvas.drawBitmap(layer.bitmap, 0f, 0f, paint)
    }

    fun deleteLayer(layerId: String): Boolean = layerStack.removeIf { it.layerId == layerId }

    fun renameLayer(layerId: String, newName: String): Boolean {
        val layer = layerStack.firstOrNull { it.layerId == layerId } ?: return false
        val name = newName.trim()
        if (name.isEmpty()) return false
        layer.layerName = name
        return true
    }

    fun setVisibility(layerId: String, visible: Boolean) {
        layerStack.firstOrNull { it.layerId == layerId }?.visible = visible
    }

    fun setOpacity(layerId: String, opacity: Int) {
        layerStack.firstOrNull { it.layerId == layerId }?.opacity = opacity.coerceIn(0, 255)
    }

    fun getLayerCount(): Int = layerStack.size

    fun exportLayersAsPngSequence(outputDirectory: File): List<File> {
        outputDirectory.mkdirs()
        return layerStack.sortedBy { it.order }.mapIndexedNotNull { index, layer ->
            val out = File(outputDirectory, "%03d_${sanitize(layer.layerName)}.png".format(index))
            if (layer.bitmap.compress(Bitmap.CompressFormat.PNG, 100, out.outputStream())) out else null
        }
    }

    fun importLayer(file: File, name: String = file.nameWithoutExtension): LayerData? {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return LayerData("layer_${UUID.randomUUID()}", name, bitmap.copy(Bitmap.Config.ARGB_8888, true), (layerStack.maxOfOrNull { it.order } ?: -1) + 1)
            .also { layerStack += it }
    }

    fun optimizeMemoryUsage(maxMemoryMb: Int) {
        var currentUsage = layerStack.sumOf { it.bitmap.byteCount.toLong() }
        val limit = maxMemoryMb.coerceAtLeast(16).toLong() * 1024L * 1024L
        while (currentUsage > limit && layerStack.size > 1) {
            val victim = layerStack.minByOrNull { it.order } ?: break
            layerStack.remove(victim)
            currentUsage -= victim.bitmap.byteCount.toLong()
        }
    }

    private fun createPaint(type: BrushEngine.BrushType, color: Int, size: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = size.coerceAtLeast(0.5f)
            this.color = color
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (type == BrushEngine.BrushType.AIRBRUSH) {
                maskFilter = android.graphics.BlurMaskFilter(strokeWidth * 1.5f, android.graphics.BlurMaskFilter.Blur.NORMAL)
            }
        }

    private fun toPorterDuff(mode: BlendMode): PorterDuff.Mode = when (mode) {
        BlendMode.SRC_OVER -> PorterDuff.Mode.SRC_OVER
        BlendMode.MULTIPLY -> PorterDuff.Mode.MULTIPLY
        BlendMode.SCREEN -> PorterDuff.Mode.SCREEN
        BlendMode.OVERLAY -> PorterDuff.Mode.OVERLAY
        BlendMode.DARKEN -> PorterDuff.Mode.DARKEN
        BlendMode.LIGHTEN -> PorterDuff.Mode.LIGHTEN
    }

    private fun sanitize(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(80).ifBlank { "layer" }
}
