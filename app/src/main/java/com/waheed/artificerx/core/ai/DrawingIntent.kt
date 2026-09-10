package com.waheed.artificerx.core.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import com.waheed.artificerx.core.runtime.Callback

/** Immutable-ish request object shared by drawing agents and raster capabilities. */
class DrawingIntent(
    val context: Context,
    val baseImage: Bitmap,
    val color: Int = Color.WHITE,
    val size: Float = 12f,
    val opacity: Float = 1f,
    val texture: Bitmap? = null,
    var prompt: String = "",
    var referenceUrl: String = "",
    var completionCallback: Callback<Bitmap>? = null,
)
