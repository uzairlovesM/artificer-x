package com.waheed.artificerx.core.diagnostics

class CrashSafe<T>(
    private val fallback: T,
    private val onFailure: (Throwable) -> Unit = {}
) {
    fun run(block: () -> T): T = try {
        block()
    } catch (t: Throwable) {
        onFailure(t)
        fallback
    }
}
