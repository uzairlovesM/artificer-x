package com.waheed.artificerx.core.platform

interface PlatformClock {
    fun nowMillis(): Long
    fun monotonicNanos(): Long
}

class SystemPlatformClock : PlatformClock {
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun monotonicNanos(): Long = System.nanoTime()
}
