package com.waheed.artificerx.runtime.capability

class CapabilityBroker(private val providers: Map<String, suspend (Map<String, Any?>) -> Any?>) {
    suspend fun invoke(name: String, arguments: Map<String, Any?> = emptyMap()): Result<Any?> {
        val provider = providers[name] ?: return Result.failure(IllegalArgumentException("Unknown capability: $name"))
        return runCatching { provider(arguments) }
    }

    fun list(): Set<String> = providers.keys
}
