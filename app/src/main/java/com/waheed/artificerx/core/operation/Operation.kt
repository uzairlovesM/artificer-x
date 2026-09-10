package com.waheed.artificerx.core.operation

import com.waheed.artificerx.core.foundation.CorrelationId
import com.waheed.artificerx.core.foundation.ExecutionId
import com.waheed.artificerx.core.foundation.MutationJournal
import com.waheed.artificerx.core.foundation.OperationFailure
import com.waheed.artificerx.core.foundation.OperationFailureKind
import com.waheed.artificerx.core.foundation.OperationState
import java.util.UUID
import kotlin.system.measureTimeMillis

@JvmInline
value class OperationId(val value: String) {
    init { require(value.isNotBlank()) }
    override fun toString(): String = value
    companion object { fun create(): OperationId = OperationId(UUID.randomUUID().toString()) }
}

data class OperationContext(
    val executionId: ExecutionId = ExecutionId.create(),
    val correlationId: CorrelationId? = null,
    val actor: String = "system",
    val metadata: Map<String, String> = emptyMap(),
) {
    init { require(actor.isNotBlank()) }
}

data class OperationOutcome<T>(
    val id: OperationId,
    val value: T?,
    val state: OperationState,
    val failure: OperationFailure? = null,
    val elapsedMillis: Long = 0L,
    val context: OperationContext = OperationContext(),
) {
    val success: Boolean get() = state == OperationState.COMMITTED
}

class OperationRunner(
    private val clock: () -> Long = System::currentTimeMillis,
    private val monotonicNanos: () -> Long = System::nanoTime,
    private val journal: MutationJournal = MutationJournal(),
) {
    fun <T> run(
        context: OperationContext = OperationContext(),
        validate: () -> Unit = {},
        verify: (T) -> Unit = {},
        block: () -> T,
    ): OperationOutcome<T> {
        val id = OperationId.create()
        val startedAt = monotonicNanos()
        journal.append(context.executionId, "operation.created", context.correlationId, clock(), mapOf("operationId" to id.value))
        var state = OperationState.VALIDATING
        return try {
            validate()
            journal.append(context.executionId, "operation.validated", context.correlationId, clock(), mapOf("operationId" to id.value))
            state = OperationState.RUNNING
            journal.append(context.executionId, "operation.running", context.correlationId, clock(), mapOf("operationId" to id.value))
            val value = block()
            state = OperationState.VERIFYING
            journal.append(context.executionId, "operation.verifying", context.correlationId, clock(), mapOf("operationId" to id.value))
            verify(value)
            state = OperationState.COMMITTED
            journal.append(context.executionId, "operation.committed", context.correlationId, clock(), mapOf("operationId" to id.value))
            OperationOutcome(id, value, state, elapsedMillis = elapsedMillis(startedAt), context = context)
        } catch (cancel: CancellationException) {
            state = OperationState.CANCELLED
            journal.append(context.executionId, "operation.cancelled", context.correlationId, clock(), mapOf("operationId" to id.value))
            OperationOutcome(id, null, state, OperationFailure(OperationFailureKind.UNKNOWN, cancel.message ?: "Operation cancelled", true, cancel::class.java.name), elapsedMillis(startedAt), context)
        } catch (t: Throwable) {
            val failure = classify(t, state)
            state = if (state == OperationState.VERIFYING) OperationState.ROLLED_BACK else OperationState.FAILED
            journal.append(context.executionId, "operation.failed", context.correlationId, clock(), mapOf("operationId" to id.value, "kind" to failure.kind.name, "state" to state.name))
            OperationOutcome(id, null, state, failure, elapsedMillis(startedAt), context)
        }
    }

    fun journal(): MutationJournal = journal

    private fun elapsedMillis(startedNanos: Long): Long = ((monotonicNanos() - startedNanos) / 1_000_000L).coerceAtLeast(0L)

    private fun classify(t: Throwable, state: OperationState): OperationFailure {
        val kind = when (t) {
            is IllegalArgumentException -> OperationFailureKind.VALIDATION
            is IllegalStateException -> if (state == OperationState.VERIFYING) OperationFailureKind.CONFLICT else OperationFailureKind.INTERNAL
            is java.util.concurrent.TimeoutException -> OperationFailureKind.TIMEOUT
            is SecurityException -> OperationFailureKind.PERMISSION
            is java.io.IOException -> OperationFailureKind.IO
            else -> OperationFailureKind.UNKNOWN
        }
        val retryable = kind == OperationFailureKind.TIMEOUT || kind == OperationFailureKind.IO
        return OperationFailure(kind, t.message ?: t::class.java.simpleName, retryable, t::class.java.name)
    }

    private class CancellationException(message: String) : RuntimeException(message)
}
