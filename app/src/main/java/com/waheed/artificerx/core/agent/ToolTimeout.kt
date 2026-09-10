package com.waheed.artificerx.core.agent
class ToolTimeout(private val clock:()->Long=System::currentTimeMillis) {
    fun expired(startedAt:Long,timeoutMillis:Long,now:Long=clock()):Boolean = now-startedAt>=timeoutMillis
    fun remaining(startedAt:Long,timeoutMillis:Long,now:Long=clock()):Long=(timeoutMillis-(now-startedAt)).coerceAtLeast(0L)
}
