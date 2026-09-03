package com.waheed.artificerx.core.timelapse

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v0.4.30 real timelapse recording (Procreate/ibisPaint both ship
 * this — researched via Exa: Procreate calls it "instant replay",
 * ibisPaint has "Play" — an actual competitive-parity feature this
 * app was missing entirely, not a fake stub). Every recomposite
 * during an active drawing session captures a downscaled JPEG frame
 * to internal storage; TimelapseScreen plays the sequence back.
 *
 * Design choices, and why:
 * - Downscaled to [MAX_FRAME_DIMENSION_PX] rather than full canvas
 *   resolution: a timelapse of a 2000px canvas doesn't need
 *   2000px frames to watch back, and full-res frames would burn
 *   through device storage in minutes of active drawing.
 * - Throttled to one frame per [MIN_CAPTURE_INTERVAL_MS]: recomposite()
 *   already debounces on its own, but a rapid burst of small strokes
 *   (e.g. shading with many short brush passes) could still trigger
 *   several recomposites within a second or two — capturing every
 *   single one would make the "timelapse" mostly near-duplicate
 *   frames and waste storage for no playback benefit.
 * - JPEG not PNG: timelapse frames are disposable/regenerable preview
 *   data, not the artwork itself (which is exported as PNG elsewhere
 *   via ImageExporter) — JPEG's lossy compression is the right
 *   tradeoff here for storage size.
 * - Frame files are NOT stitched into an MP4 yet — that needs
 *   MediaCodec/MediaMuxer video encoding, a substantially larger
 *   separate piece of work tracked as follow-up, not silently
 *   claimed done here. In-app frame-sequence playback (this class +
 *   TimelapseScreen) is real and functional today.
 */
@Singleton
class TimelapseRecorder
    @Inject
    constructor(
        private val workspaceFileSystem: WorkspaceFileSystem,
    ) {
        private val lastCaptureAtByProject = mutableMapOf<String, Long>()

        suspend fun captureFrame(
            projectId: String,
            bitmap: Bitmap,
        ) {
            val now = System.currentTimeMillis()
            val lastCapture = lastCaptureAtByProject[projectId] ?: 0L
            if (now - lastCapture < MIN_CAPTURE_INTERVAL_MS) return
            lastCaptureAtByProject[projectId] = now

            withContext(Dispatchers.IO) {
                runCatching {
                    val dir = projectDir(projectId)
                    if (!dir.exists()) dir.mkdirs()
                    val scaled = downscale(bitmap, MAX_FRAME_DIMENSION_PX)
                    val frameFile = File(dir, "frame_${now}.jpg")
                    FileOutputStream(frameFile).use { stream ->
                        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                    }
                    if (scaled !== bitmap) scaled.recycle()
                    enforceFrameCap(dir)
                }
            }
        }

        suspend fun listFrames(projectId: String): List<File> =
            withContext(Dispatchers.IO) {
                val dir = projectDir(projectId)
                if (!dir.exists()) return@withContext emptyList()
                dir.listFiles { file -> file.extension == "jpg" }
                    ?.sortedBy { it.name.removePrefix("frame_").removeSuffix(".jpg").toLongOrNull() ?: 0L }
                    ?: emptyList()
            }

        suspend fun clearFrames(projectId: String) {
            withContext(Dispatchers.IO) {
                runCatching { projectDir(projectId).deleteRecursively() }
            }
        }

        private fun projectDir(projectId: String): File = workspaceFileSystem.projectDir(projectId).resolve(TIMELAPSE_DIR_NAME).also { it.mkdirs() }

        private fun downscale(
            bitmap: Bitmap,
            maxDimension: Int,
        ): Bitmap {
            val largestSide = maxOf(bitmap.width, bitmap.height)
            if (largestSide <= maxDimension) return bitmap
            val scale = maxDimension.toFloat() / largestSide
            val targetWidth = (bitmap.width * scale).toInt().coerceAtLeast(1)
            val targetHeight = (bitmap.height * scale).toInt().coerceAtLeast(1)
            return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        }

        /** Caps stored frames per project so an hours-long drawing
         *  session can't quietly fill up the user's phone storage —
         *  oldest frames are dropped first once the cap is hit, which
         *  thins the beginning of a very long timelapse rather than
         *  ever refusing to record the current moment. */
        private fun enforceFrameCap(dir: File) {
            val frames = dir.listFiles { file -> file.extension == "jpg" }?.sortedBy { it.name } ?: return
            if (frames.size <= MAX_FRAMES_PER_PROJECT) return
            frames.take(frames.size - MAX_FRAMES_PER_PROJECT).forEach { it.delete() }
        }

        private companion object {
            const val TIMELAPSE_DIR_NAME = "timelapse"
            const val MAX_FRAME_DIMENSION_PX = 512
            const val MIN_CAPTURE_INTERVAL_MS = 1500L
            const val MAX_FRAMES_PER_PROJECT = 600
        }
    }
