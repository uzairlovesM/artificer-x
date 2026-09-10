package com.waheed.artificerx.core.runtime
class FrameRateController(private val targetFps:Float=60f) {
    init{require(targetFps>0f)}
    private var lastFrame=0L
    fun shouldRender(nowNanos:Long):Boolean {
        val interval=(1_000_000_000.0/targetFps).toLong()
        if(lastFrame==0L||nowNanos-lastFrame>=interval){lastFrame=nowNanos;return true}
        return false
    }
}
