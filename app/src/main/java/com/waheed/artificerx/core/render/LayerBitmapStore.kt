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

        // ── Undo/Redo (Section 26 follow-up) ──
        // A history entry is a full snapshot of every layer's bitmap at
        // that point in time, taken right BEFORE a mutating operation is
        // applied. This is deliberately simple (whole-bitmap copies, not
        // per-stroke diffs) because ArtificerX's canvases are small
        // (Section 171's budget-device target keeps them well under
        // 2048x2048) and history depth is capped, so the memory cost is
        // bounded and predictable — a diff-based undo system would save
        // memory but adds real complexity (dirty-rect tracking across
        // every one of CanvasCompositor's ~15 draw operations) for a
        // problem this project doesn't actually have yet.
        //
        // Redo stack is cleared on every new push, matching the standard
        // "new action after undo discards the redo branch" behavior every
        // paint app uses (Procreate, Photoshop, etc.) — this is what
        // artists actually expect, not a branching history tree.
        private val undoStack = ArrayDeque<Map<String, Bitmap>>()
        private val redoStack = ArrayDeque<Map<String, Bitmap>>()

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

        /** v0.4.30 resize_canvas tool backing: replaces every given
         *  layer's bitmap with a new-dimension one, drawing the old
         *  content anchored at (0,0) — this naturally crops content that
         *  falls outside the new bounds and pads with transparency where
         *  the new canvas is larger, rather than rescaling pixels (see
         *  ResizeCanvas tool doc in ToolRegistry for why crop/pad and not
         *  rescale is the correct default here). Clears the undo/redo
         *  stacks: their snapshots are old-dimension bitmaps that can no
         *  longer be validly restored into the new canvas size, and
         *  silently keeping them around would corrupt the very next undo
         *  rather than fail loudly. */
        fun resizeAllLayers(
            layerIds: List<String>,
            newWidthPx: Int,
            newHeightPx: Int,
        ) {
            layerIds.forEach { layerId ->
                val old = bitmaps[layerId]
                val resized = Bitmap.createBitmap(newWidthPx, newHeightPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(resized)
                if (old != null) {
                    canvas.drawBitmap(old, 0f, 0f, null)
                    old.recycle()
                }
                bitmaps[layerId] = resized
                canvases[layerId] = canvas
            }
            undoStack.clear()
            redoStack.clear()
        }

        /** Call BEFORE any pixel-mutating operation (drawPath, drawShape,
         *  fillRegion, applyFilter, etc.) so that state can be restored by
         *  [undo]. Snapshots every current layer bitmap — cheap relative
         *  to the mutation itself since Bitmap.copy is a single native
         *  memcpy, not a pixel-by-pixel loop. Pushing a new undo entry
         *  always clears the redo stack (see class doc). */
        fun pushUndoSnapshot() {
            val snapshot = bitmaps.mapValues { (_, bmp) -> bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, true) }
            undoStack.addLast(snapshot)
            if (undoStack.size > MAX_HISTORY_DEPTH) {
                undoStack.removeFirst().values.forEach { it.recycle() }
            }
            redoStack.forEach { entry -> entry.values.forEach { it.recycle() } }
            redoStack.clear()
        }

        /** Restores the most recent undo snapshot, pushing the
         *  pre-restore state onto the redo stack first. Returns false if
         *  there's nothing to undo. Bitmaps are restored in place (drawn
         *  into the existing Bitmap objects) rather than swapping bitmap
         *  references, so any other holder of the old Bitmap instance
         *  (e.g. a Compose ImageBitmap wrapper mid-recomposition) still
         *  sees a coherent object identity. */
        fun undo(): Boolean {
            val previous = undoStack.removeLastOrNull() ?: return false
            val currentSnapshot = bitmaps.mapValues { (_, bmp) -> bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, true) }
            redoStack.addLast(currentSnapshot)
            restoreSnapshot(previous)
            return true
        }

        /** Re-applies a state that was undone. Returns false if there's
         *  nothing to redo. */
        fun redo(): Boolean {
            val next = redoStack.removeLastOrNull() ?: return false
            val currentSnapshot = bitmaps.mapValues { (_, bmp) -> bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, true) }
            undoStack.addLast(currentSnapshot)
            restoreSnapshot(next)
            return true
        }

        private fun restoreSnapshot(snapshot: Map<String, Bitmap>) {
            // Remove any layer that exists now but didn't exist in the
            // snapshot (e.g. a layer added after this history point).
            val layersToRemove = bitmaps.keys - snapshot.keys
            layersToRemove.forEach { layerId ->
                bitmaps.remove(layerId)?.recycle()
                canvases.remove(layerId)
            }

            snapshot.forEach { (layerId, snapshotBitmap) ->
                val target = bitmaps[layerId]
                if (target != null && target.width == snapshotBitmap.width && target.height == snapshotBitmap.height) {
                    val canvas = canvases[layerId] ?: Canvas(target).also { canvases[layerId] = it }
                    canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                    canvas.drawBitmap(snapshotBitmap, 0f, 0f, null)
                } else {
                    // Layer didn't exist (was deleted) or changed size
                    // (e.g. crop_canvas) since this snapshot — recreate it
                    // fresh from the snapshot bitmap directly.
                    target?.recycle()
                    val restored = snapshotBitmap.copy(snapshotBitmap.config ?: Bitmap.Config.ARGB_8888, true)
                    bitmaps[layerId] = restored
                    canvases[layerId] = Canvas(restored)
                }
            }
        }

        fun canUndo(): Boolean = undoStack.isNotEmpty()

        fun canRedo(): Boolean = redoStack.isNotEmpty()

        fun undoStackDepth(): Int = undoStack.size

        fun redoStackDepth(): Int = redoStack.size

        /** Clears all history — must be called whenever a project is
         *  loaded or switched, since undoing "past" a freshly loaded
         *  project's starting point makes no sense and would otherwise
         *  silently restore bitmaps from a completely different project. */
        fun clearHistory() {
            undoStack.forEach { entry -> entry.values.forEach { it.recycle() } }
            redoStack.forEach { entry -> entry.values.forEach { it.recycle() } }
            undoStack.clear()
            redoStack.clear()
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
            clearHistory()
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

        /** v0.4.30 Transform tool: applies an incremental Matrix transform
         *  (translate + uniform scale + rotate about a pivot) directly to
         *  a layer's bitmap in place — mirrors [flipLayer]'s
         *  createBitmap(source, matrix, filter=true) approach so transform
         *  quality matches the existing flip operation. Called once per
         *  gesture-update frame from StudioViewModel.transformActiveLayer
         *  with that frame's small delta (not the total drag), so the
         *  user sees the layer move/scale/rotate live under their finger
         *  rather than only snapping into place on release. */
        fun transformLayer(
            layerId: String,
            translateX: Float,
            translateY: Float,
            scaleFactor: Float,
            rotationDegrees: Float,
            pivotX: Float,
            pivotY: Float,
        ): Boolean {
            val source = bitmaps[layerId] ?: return false
            val matrix =
                android.graphics.Matrix().apply {
                    postTranslate(translateX, translateY)
                    postScale(scaleFactor, scaleFactor, pivotX + translateX, pivotY + translateY)
                    postRotate(rotationDegrees, pivotX + translateX, pivotY + translateY)
                }
            // Draw through a same-size scratch bitmap via Canvas.drawBitmap(
            // source, matrix, paint) rather than Bitmap.createBitmap(source,
            // ..., matrix, true) — the latter auto-crops/expands its output
            // to the transformed content's bounding box, which for any
            // rotation no longer matches the original canvas's coordinate
            // space and would visibly shift the layer on every rotate.
            // Compositing through a fixed-size scratch keeps the layer
            // anchored to the same canvas bounds the rest of the app
            // assumes everywhere else.
            val scratch = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            val scratchCanvas = Canvas(scratch)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
            scratchCanvas.drawBitmap(source, matrix, paint)

            val canvas = canvases[layerId] ?: return false
            canvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
            canvas.drawBitmap(scratch, 0f, 0f, null)
            scratch.recycle()
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

        companion object {
            // Capped so a very long freehand session can't grow undo
            // memory unboundedly — 25 steps back matches what most
            // mobile paint apps offer by default (Procreate: unlimited
            // but disk-backed; budget mobile apps: 20-30 in-memory).
            private const val MAX_HISTORY_DEPTH = 25
        }
    }
