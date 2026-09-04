package com.waheed.artificerx.core.util

import kotlin.math.roundToInt

object StudioMath {
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b-a)*t.coerceIn(0f, 1f)
    fun snap(value: Float, grid: Float): Float = if (grid <= 0f) value else (value/grid).roundToInt()*grid
    fun aspect(width: Int, height: Int): Float = if (height == 0) 0f else width.toFloat()/height
    fun clampInt(value: Int, min: Int, max: Int) = value.coerceIn(min, max)
}
