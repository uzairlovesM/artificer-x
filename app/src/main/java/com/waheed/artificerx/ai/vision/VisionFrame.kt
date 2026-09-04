package com.waheed.artificerx.ai.vision

import android.graphics.Bitmap
import java.time.Instant

data class VisionFrame(
    val bitmap: Bitmap,
    val source: VisionSource,
    val capturedAt: Instant = Instant.now(),
    val revision: Long = 0L,
    val width: Int = bitmap.width,
    val height: Int = bitmap.height
)

enum class VisionSource { CANVAS, IMPORT, CAMERA, ARTIFACT, WEB_REFERENCE }
