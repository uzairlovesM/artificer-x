package com.waheed.artificerx.util.concurrency

class Backpressure(private val maxInFlight: Int) {
    private var inFlight = 0
    @Synchronized fun tryAcquire(): Boolean = if(inFlight<maxInFlight){inFlight++;true}else false
    @Synchronized fun release(){if(inFlight>0)inFlight--}
}
