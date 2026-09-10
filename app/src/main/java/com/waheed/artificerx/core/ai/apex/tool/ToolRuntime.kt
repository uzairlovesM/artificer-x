package com.waheed.artificerx.core.ai.apex.tool

import java.util.concurrent.atomic.AtomicLong

data class ToolSpec(val id: String, val version: Int, val dangerous: Boolean, val idempotent: Boolean, val timeoutMs: Long = 30_000L)
data class ToolRequest(val toolId: String, val input: Map<String, String>, val correlationId: String)
data class ToolResult(val success: Boolean, val output: Map<String, String>, val error: String? = null, val elapsedMs: Long = 0L)

class ToolRuntime {
    private val specs = LinkedHashMap<String, ToolSpec>()
    private val executions = AtomicLong(0)
    @Synchronized fun register(spec: ToolSpec) { require(spec.id.isNotBlank()); specs[spec.id] = spec }
    @Synchronized fun count(): Int = specs.size
    @Synchronized fun spec(id: String): ToolSpec? = specs[id]
    fun execute(request: ToolRequest, handler: (ToolRequest) -> Map<String, String>): ToolResult {
        val spec = spec(request.toolId) ?: return ToolResult(false, emptyMap(), "unknown_tool")
        val started = System.nanoTime()
        return try {
            val out = handler(request)
            executions.incrementAndGet()
            val elapsed = (System.nanoTime() - started) / 1_000_000
            if (elapsed > spec.timeoutMs) ToolResult(false, emptyMap(), "timeout", elapsed) else ToolResult(true, out, elapsedMs = elapsed)
        } catch (t: Throwable) {
            ToolResult(false, emptyMap(), t.message ?: t::class.simpleName, (System.nanoTime() - started) / 1_000_000)
        }
    }
}
