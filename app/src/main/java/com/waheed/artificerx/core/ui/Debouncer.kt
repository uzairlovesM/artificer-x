package com.waheed.artificerx.core.ui
class Debouncer(private val delayMillis:Long) {
    init{require(delayMillis>=0)}
    private var generation=0L
    @Synchronized fun submit(block:()->Unit):Long { generation++; return generation }
    @Synchronized fun isCurrent(token:Long)=token==generation
}
