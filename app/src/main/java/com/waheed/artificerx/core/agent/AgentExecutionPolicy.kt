package com.waheed.artificerx.core.agent

/** Turn-level guardrails that scale with intent without removing the user's quality preset. */
object AgentExecutionPolicy {
    data class Budget(
        val maxIterations: Int,
        val maxToolCalls: Int,
        val artifactRequired: Boolean,
        val webAllowed: Boolean,
    )

    fun budget(prompt: String, configuredIterations: Int, networkAvailable: Boolean): Budget {
        val lower = prompt.lowercase()
        val artifactRequired = listOf("zip", "file", "download", "export", "image", "pdf", "docx", "project").any(lower::contains)
        val complex = listOf("full", "complete", "build", "research", "multi", "all", "everything", "deep", "audit").count(lower::contains)
        // No artificial application-side ceiling. Provider limits, model context,
        // Android resources, and explicit cancellation remain the real boundaries.
        val iterations = UnboundedExecutionPolicy.MAX_APPLICATION_ITERATIONS
        val toolCalls = UnboundedExecutionPolicy.MAX_APPLICATION_TOOL_CALLS
        return Budget(iterations, toolCalls, artifactRequired, networkAvailable)
    }
}
