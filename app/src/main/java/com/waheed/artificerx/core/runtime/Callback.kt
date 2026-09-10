package com.waheed.artificerx.core.runtime

/** Lightweight callback contract for legacy and agent-facing drawing operations. */
interface Callback<T> {
    fun onSuccess(value: T)
    fun onFailure(error: Throwable)
}
