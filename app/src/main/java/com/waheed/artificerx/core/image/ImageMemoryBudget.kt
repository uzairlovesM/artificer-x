package com.waheed.artificerx.core.image

class ImageMemoryBudget(private val maxBytes: Long) {
    init { require(maxBytes > 0) }
    private var reserved = 0L

    @Synchronized fun tryReserve(bytes: Long): Boolean {
        if (bytes <= 0) return true
        if (reserved > maxBytes - bytes) return false
        reserved += bytes
        return true
    }

    @Synchronized fun release(bytes: Long) { reserved = (reserved - bytes.coerceAtLeast(0L)).coerceAtLeast(0L) }
    @Synchronized fun remaining(): Long = (maxBytes - reserved).coerceAtLeast(0L)
}
