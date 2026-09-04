package com.waheed.artificerx.core.ai.intelligence

import com.waheed.artificerx.core.builtin.BuiltinRecipeCatalog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReasoningLoop @Inject constructor(
    private val planner: IntelligencePlanner,
    private val catalog: BuiltinRecipeCatalog,
) {
    data class Decision(
        val goal: String,
        val candidateRecipes: List<String>,
        val verificationRequired: Boolean,
        val missingInformation: List<String>,
    )

    fun decide(task: String): Decision {
        val candidates = planner.recommend(task, 8)
        val missing = buildList {
            if (task.isBlank()) add("task description")
            if (task.contains("file", true) && !task.contains("path", true)) add("workspace path")
            if (task.contains("copy", true) && !task.contains("destination", true)) add("destination")
        }
        return Decision(task.trim(), candidates.map { it.id }, planner.plan(task).any { it.contains("verify", true) }, missing)
    }

    fun catalogLoaded(): Boolean = catalog.count() >= 1000
}
