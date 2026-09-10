package com.waheed.artificerx.core.ai

import android.content.Context
import android.graphics.Bitmap

/** Intent used by the lightweight drawing agent path. */
data class DrawingIntent(
    val context: Context,
    val baseImage: Bitmap,
    val color: Int = android.graphics.Color.BLACK,
    val size: Float = 4f,
    val opacity: Float = 1f,
    val texture: Bitmap? = null,
    val paths: List<android.graphics.Path> = emptyList(),
    val completionCallback: com.waheed.artificerx.core.runtime.Callback<Bitmap>? = null,
)
