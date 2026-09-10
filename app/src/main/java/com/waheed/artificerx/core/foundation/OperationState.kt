package com.waheed.artificerx.core.foundation

enum class OperationState {
    CREATED,
    VALIDATING,
    RUNNING,
    VERIFYING,
    COMMITTED,
    ROLLED_BACK,
    FAILED,
    CANCELLED,
}

enum class OperationFailureKind {
    VALIDATION,
    CONFLICT,
    PERMISSION,
    RESOURCE,
    TIMEOUT,
    IO,
    INTERNAL,
    UNKNOWN,
}

data class OperationFailure(
    val kind: OperationFailureKind,
    val message: String,
    val retryable: Boolean,
    val causeClass: String? = null,
) {
    init { require(message.isNotBlank()) }
}
