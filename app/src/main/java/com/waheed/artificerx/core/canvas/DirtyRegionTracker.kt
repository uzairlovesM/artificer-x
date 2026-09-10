package com.waheed.artificerx.core.canvas

import kotlin.math.max
import kotlin.math.min

data class RectFInt(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val area: Long get() = max(0, right - left).toLong() * max(0, bottom - top).toLong()
    fun union(other: RectFInt) = RectFInt(
        min(left, other.left), min(top, other.top), max(right, other.right), max(bottom, other.bottom)
    )
}

class DirtyRegionTracker {
    private var region: RectFInt? = null
    fun mark(rect: RectFInt) { region = region?.union(rect) ?: rect }
    fun consume(): RectFInt? = region.also { region = null }
    fun peek(): RectFInt? = region
}
