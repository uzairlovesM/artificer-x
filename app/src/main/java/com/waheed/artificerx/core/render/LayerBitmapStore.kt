package com.waheed.artificerx.core.render

import android.graphics.Bitmap
import android.graphics.Canvas
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 26 Layer System's actual pixel storage. One ARGB_8888 Bitmap
 * per layer ID, held in memory for the active project. This is
 * deliberately separate from CanvasLayer (the metadata model in
 * StudioViewModel's state) — metadata is cheap to copy through
 * StateFlow updates on every recomposition, but a Bitmap must never be
 * duplicated on every state emission or memory blows up fast on a
 * budget device (Section 171's SM-A325F / Helio G85 target).
 *
 * Thread-safety: draw operations arrive from the agent's IO-dispatched
 * coroutine but must mutate on the main thread since Android's Canvas
 * drawing APIs are not thread-safe for concurrent access to the same
 * Bitmap; ToolExecutor already dispatches its execution block onto
 * Dispatchers.Main.immediate before calling into this store.
 */
@Singleton
class LayerBitmapStore
    @Inject
    constructor() {
        private val bitmaps = ConcurrentHashMap<String, Bitmap>()
        private val canvases = ConcurrentHashMap<String, Canvas>()

        fun ensureLayer(
            layerId: String,
            widthPx: Int,
            heightPx: Int,
        ): Bitmap =
            bitmaps.getOrPut(layerId) {
                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                canvases[layerId] = Canvas(bitmap)
                bitmap
            }

        fun getBitmap(layerId: String): Bitmap? = bitmaps[layerId]

        fun getCanvas(layerId: String): Canvas? = canvases[layerId]

        fun removeLayer(layerId: String) {
            bitmaps.remove(layerId)?.recycle()
            canvases.remove(layerId)
        }

        fun clearLayer(layerId: String) {
            val bitmap = bitmaps[layerId] ?: return
            canvases[layerId]?.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
        }

        fun clearAll() {
            bitmaps.values.forEach { it.recycle() }
            bitmaps.clear()
            canvases.clear()
        }

        /** Replaces a layer's bitmap with a cropped sub-region of itself.
         *  Coordinates are clamped to the source bitmap's bounds so an
         *  out-of-range crop rect from the LLM degrades to "crop what
         *  overlaps" rather than crashing. Used by the crop_canvas tool,
         *  called once per layer since every layer must stay the same
         *  dimensions as the canvas. */
        fun cropLayer(
            layerId: String,
            cropX: Int,
            cropY: Int,
            cropWidth: Int,
            cropHeight: Int,
        ): Boolean {
            val source = bitmaps[layerId] ?: return false
            val safeX = cropX.coerceIn(0, (source.width - 1).coerceAtLeast(0))
            val safeY = cropY.coerceIn(0, (source.height - 1).coerceAtLeast(0))
            val safeWidth = cropWidth.coerceIn(1, source.width - safeX)
            val safeHeight = cropHeight.coerceIn(1, source.height - safeY)

            val cropped = Bitmap.createBitmap(source, safeX, safeY, safeWidth, safeHeight)
            val destination = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
            val destCanvas = Canvas(destination)
            destCanvas.drawBitmap(cropped, 0f, 0f, null)

            bitmaps[layerId]?.recycle()
            bitmaps[layerId] = destination
            canvases[layerId] = destCanvas
            return true
        }

        fun hasLayer(layerId: String): Boolean = bitmaps.containsKey(layerId)

        /** Copies an existing layer's pixels into a brand-new bitmap entry
         *  under [newLayerId]. Returns false if the source layer doesn't
         *  exist. Used by the duplicate_layer tool — a plain
         *  Bitmap.copy(config, true) rather than re-drawing, since it's both
         *  simpler and guaranteed pixel-identical to the source. */
        fun duplicateLayer(
            sourceLayerId: String,
            newLayerId: String,
        ): Boolean {
            val source = bitmaps[sourceLayerId] ?: return false
            val copy = source.copy(Bitmap.Config.ARGB_8888, true)
            bitmaps[newLayerId] = copy
            canvases[newLayerId] = Canvas(copy)
            return true
        }

        /** Mirrors a layer's pixels in place, horizontally or vertically.
         *  Used by the flip_layer tool. */
        fun flipLayer(
            layerId: String,
            horizontal: Boolean,
            vertical: Boolean,
        ): Boolean {
            val source = bitmaps[layerId] ?: return false
            if (!horizontal && !vertical) return true
            val matrix =
                android.graphics.Matrix().apply {
                    postScale(
                        if (horizontal) -1f else 1f,
                        if (vertical) -1f else 1f,
                        source.width / 2f,
                        source.height / 2f,
                    )
                }
            val flipped = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            val canvas = canvases[layerId] ?: return false
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(flipped, 0f, 0f, null)
            return true
        }

        /** Total memory footprint estimate — used by Section 137's
         *  thermal/memory-awareness gating before allowing new layers. */
        fun estimateTotalBytes(): Long = bitmaps.values.sumOf { it.byteCount.toLong() }
    }
