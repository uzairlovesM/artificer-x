package com.waheed.artificerx.runtime.capability

class AutonomyController(private val broker: CapabilityBroker) {
    suspend fun execute(tool: String, args: Map<String, Any?>): Result<Any?> = broker.invoke(tool, args)
    fun capabilities(): Set<String> = broker.list()
}
