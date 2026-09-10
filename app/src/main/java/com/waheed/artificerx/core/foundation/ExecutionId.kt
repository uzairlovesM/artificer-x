package com.waheed.artificerx.core.foundation

import java.util.UUID

@JvmInline
value class ExecutionId(val value: String) {
    init { require(value.isNotBlank()) }
    override fun toString(): String = value

    companion object {
        fun create(): ExecutionId = ExecutionId(UUID.randomUUID().toString())
    }
}

@JvmInline
value class CorrelationId(val value: String) {
    init { require(value.isNotBlank()) }
    override fun toString(): String = value

    companion object {
        fun create(): CorrelationId = CorrelationId(UUID.randomUUID().toString())
    }
}
