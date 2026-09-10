package com.waheed.artificerx.core.ai.apex.workflow

import com.waheed.artificerx.core.ai.apex.agent.AgentTaskGraph

data class WorkflowState(val completed: Set<String> = emptySet(), val failed: Set<String> = emptySet(), val outputs: Map<String, String> = emptyMap())
data class WorkflowStepResult(val nodeId: String, val success: Boolean, val output: String = "", val error: String? = null)

class AutonomousWorkflowEngine {
    fun run(graph: AgentTaskGraph, initial: WorkflowState = WorkflowState(), handler: (String, String) -> WorkflowStepResult): WorkflowState {
        var state = initial
        while (true) {
            val ready = graph.ready(state.completed)
            if (ready.isEmpty()) break
            var progressed = false
            for (node in ready) {
                val result = handler(node.id, node.description)
                if (result.success) {
                    state = state.copy(completed = state.completed + node.id, outputs = state.outputs + (node.id to result.output))
                } else {
                    state = state.copy(failed = state.failed + node.id, outputs = state.outputs + (node.id to (result.error ?: "failed")))
                }
                progressed = true
            }
            if (!progressed) break
        }
        return state
    }
}
