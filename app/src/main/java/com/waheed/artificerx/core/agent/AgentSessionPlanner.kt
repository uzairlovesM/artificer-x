package com.waheed.artificerx.core.agent

data class AgentPlanStep(val id: String, val title: String, val kind: String, val dependsOn: List<String> = emptyList())

data class AgentPlan(val intent: String, val steps: List<AgentPlanStep>, val completionCriteria: List<String>)

object AgentSessionPlanner {
    fun plan(request: String): AgentPlan {
        val lower = request.lowercase()
        val creative = listOf("draw", "paint", "illustration", "anime", "manga", "design").any(lower::contains)
        val files = listOf("zip", "file", "project", "code", "app", "folder").any(lower::contains)
        val outputs = buildList {
            add(AgentPlanStep("understand", "Understand intent", "analysis"))
            add(AgentPlanStep("context", "Load relevant project context", "context", listOf("understand")))
            if (creative) add(AgentPlanStep("compose", "Compose visual plan", "creative", listOf("context")))
            if (files) add(AgentPlanStep("materialize", "Materialize files/artifacts", "artifact", listOf(if (creative) "compose" else "context")))
            add(AgentPlanStep("verify", "Inspect result and validate outputs", "verification", listOf(if (files) "materialize" else if (creative) "compose" else "context")))
            add(AgentPlanStep("finalize", "Summarize only verified results", "final", listOf("verify")))
        }
        return AgentPlan(request.take(160), outputs, listOf("No claimed artifact exists without a successful tool result", "Errors trigger repair guidance", "Final response reflects verified state"))
    }
}
