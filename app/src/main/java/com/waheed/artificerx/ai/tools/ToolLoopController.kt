package com.waheed.artificerx.ai.tools

class ToolLoopController(private val executor: suspend (ToolInvocation)->ToolResult) {
    suspend fun run(invocations: List<ToolInvocation>): List<ToolResult> {
        val results = ArrayList<ToolResult>(invocations.size)
        for (invocation in invocations) results += executor(invocation)
        return results
    }
}
