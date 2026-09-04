package com.waheed.artificerx.core.ai.intelligence

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntelligencePolicy @Inject constructor() {
    fun objectiveStack(task: String): List<String> = listOf(
        "understand user intent before acting",
        "identify missing constraints and required artifacts",
        "select the narrowest real toolset that can complete the task",
        "prefer deterministic local inspection before speculative generation",
        "execute in observable checkpoints",
        "verify outputs against the requested semantics",
        "repair failed or low-confidence output automatically",
        "persist useful artifacts and provenance",
        "summarize completed work with explicit limitations",
    ) + task.takeIf { it.isNotBlank() }?.let { listOf("task=$it") }.orEmpty()

    fun shouldVerify(kind: String): Boolean = kind.lowercase() !in setOf("noop", "read_only_preview")
}
