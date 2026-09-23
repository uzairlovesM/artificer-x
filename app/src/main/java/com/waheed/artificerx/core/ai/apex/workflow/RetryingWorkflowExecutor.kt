package com.waheed.artificerx.core.ai.apex.workflow

import kotlinx.coroutines.delay

/**
 * Coroutine-friendly bounded retry helper. Retries never block a UI/agent thread.
 */
class RetryingWorkflowExecutor(
    private val maxAttempts: Int = 3,
    private val backoffMs: Long = 10L,
) {
    suspend fun <T> run(block: suspend () -> T): Result<T> {
        require(maxAttempts > 0) { "maxAttempts must be positive" }
        require(backoffMs >= 0L) { "backoffMs must be non-negative" }

        var last: Result<T> = Result.failure(IllegalStateException("not_run"))
        repeat(maxAttempts) { attempt ->
            last = runCatching { block() }
            if (last.isSuccess) return last
            if (attempt + 1 < maxAttempts && backoffMs > 0L) {
                delay(backoffMs * (attempt + 1L))
            }
        }
        return last
    }
}
