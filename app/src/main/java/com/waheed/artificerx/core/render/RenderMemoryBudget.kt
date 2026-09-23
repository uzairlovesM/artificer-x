package com.waheed.artificerx.core.render

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import android.util.LruCache
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** Process-aware bitmap budget used by heavy preview/thumbnail paths. */
@Singleton
class RenderMemoryBudget @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val memoryClassMb = runCatching {
        context.getSystemService(ActivityManager::class.java)?.memoryClass ?: 256
    }.getOrDefault(256)

    val maxBytes: Long = max(32L * 1024L * 1024L, memoryClassMb.toLong() * 1024L * 1024L / 5L)

    private val cache = object : LruCache<String, Bitmap>((maxBytes / 4L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.allocationByteCount.coerceAtMost(Int.MAX_VALUE)
    }

    @Synchronized
    fun put(key: String, bitmap: Bitmap) {
        if (bitmap.isRecycled || bitmap.allocationByteCount > maxBytes) return
        val previous = cache.put(key, bitmap)
        if (previous != null && previous !== bitmap && !previous.isRecycled) previous.recycle()
    }

    @Synchronized
    fun get(key: String): Bitmap? = cache.get(key)

    @Synchronized
    fun evict(key: String) {
        cache.remove(key)?.let { if (!it.isRecycled) it.recycle() }
    }

    @Synchronized
    fun clear() {
        cache.evictAll()
    }

    fun snapshot(): BudgetSnapshot = synchronized(this) {
        BudgetSnapshot(maxBytes = maxBytes, cacheBytes = cache.snapshot().values.sumOf { it.allocationByteCount.toLong() })
    }
}

data class BudgetSnapshot(
    val maxBytes: Long,
    val cacheBytes: Long,
)
