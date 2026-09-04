package com.waheed.artificerx.util.concurrency

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

class SingleFlight<K, V> {
    private val mutex = Mutex()
    private val active = mutableMapOf<K, kotlinx.coroutines.Deferred<V>>()
    suspend fun run(key: K, block: suspend () -> V): V {
        val existing = mutex.withLock { active[key] }
        if (existing != null) return existing.await()
        val deferred = CoroutineScope(kotlinx.coroutines.Dispatchers.Default).async { block() }
        mutex.withLock { active[key] = deferred }
        return try { deferred.await() } finally { mutex.withLock { if (active[key] === deferred) active.remove(key) } }
    }
}
