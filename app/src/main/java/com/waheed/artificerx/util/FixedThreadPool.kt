package com.waheed.artificerx.util

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FixedThreadPool(private val maxThreads: Int = 4) {
    init { require(maxThreads > 0) }
    private val pool: ExecutorService = Executors.newFixedThreadPool(maxThreads)

    fun <T> execute(block: () -> T): CompletableFuture<T> =
        CompletableFuture.supplyAsync(block, pool)

    fun shutdown() { pool.shutdown() }
}
