package com.waheed.artificerx.core.operation

import java.util.UUID

data class OperationId(val value: String = UUID.randomUUID().toString()) {
    override fun toString(): String = value
}

data class OperationResult<T>(
    val id: OperationId,
    val value: T?,
    val success: Boolean,
    val error: String? = null,
    val elapsedMillis: Long = 0L
)

class OperationRunner(private val clock: () -> Long = System::currentTimeMillis) {
    inline fun <T> run(block: () -> T): OperationResult<T> {
        val id = OperationId()
        val started = clock()
        return try {
            OperationResult(id, block(), true, elapsedMillis = clock() - started)
        } catch (t: Throwable) {
            OperationResult(id, null, false, t.message ?: t::class.java.simpleName, clock() - started)
        }
    }
}
