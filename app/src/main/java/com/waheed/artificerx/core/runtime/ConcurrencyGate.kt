package com.waheed.artificerx.core.runtime
class ConcurrencyGate(private val limit:Int) {
    init{require(limit>0)}
    private val permits=kotlinx.coroutines.sync.Semaphore(limit)
    suspend fun <T> withPermit(block:suspend()->T):T { permits.acquire(); return try{block()}finally{permits.release()} }
}
