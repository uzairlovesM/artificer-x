package com.waheed.artificerx.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class FixedThreadPool(private val maxThreads: Int = 4) {
    private val pool = Executors.newFixedThreadPool(maxThreads)
    private val mutex = Mutex()

    fun execute<T>(block: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync {
            mutex.withLock(block())
        }, pool
    }

    fun shutdown() {
        pool.shutdown()
    }
}
