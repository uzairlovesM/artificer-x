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
        val iterations = (configuredIterations + complex * 2 + if (artifactRequired) 2 else 0).coerceIn(2, 96)
        return Budget(iterations, (iterations * 3).coerceIn(12, 192), artifactRequired, networkAvailable)
    }
}
