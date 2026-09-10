package com.waheed.artificerx.core.ai.planning

enum class PlanStepKind { THINK, TOOL, VALIDATE, TRANSFORM, RESPOND }

data class PlanStep(
    val id: String,
    val kind: PlanStepKind,
    val title: String,
    val dependencies: List<String> = emptyList(),
    val optional: Boolean = false
)

class ExecutionPlan(private val steps: List<PlanStep>) {
    init {
        val ids = steps.map { it.id }
        require(ids.size == ids.toSet().size)
        require(steps.all { it.dependencies.all(ids::contains) })
    }

    fun topologicalOrder(): List<PlanStep> {
        val remaining = steps.associateBy { it.id }.toMutableMap()
        val completed = mutableSetOf<String>()
        val output = mutableListOf<PlanStep>()
        while (remaining.isNotEmpty()) {
            val ready = remaining.values.filter { it.dependencies.all(completed::contains) }
            if (ready.isEmpty()) error("Execution plan contains a dependency cycle")
            ready.forEach { output += it; completed += it.id; remaining.remove(it.id) }
        }
        return output
    }
}
