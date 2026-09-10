package com.waheed.artificerx.core.ai.apex.workflow

class RetryingWorkflowExecutor(private val maxAttempts: Int = 3, private val backoffMs: Long = 10L) {
    fun <T> run(block: () -> T): Result<T> {
        require(maxAttempts > 0)
        var last: Result<T> = Result.failure(IllegalStateException("not_run"))
        repeat(maxAttempts) { attempt ->
            last = runCatching(block)
            if (last.isSuccess) return last
            if (attempt + 1 < maxAttempts) Thread.sleep(backoffMs * (attempt + 1))
        }
        return last
    }
}
