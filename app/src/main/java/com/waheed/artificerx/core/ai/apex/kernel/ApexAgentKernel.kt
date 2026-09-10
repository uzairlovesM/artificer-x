package com.waheed.artificerx.core.ai.apex.kernel

import com.waheed.artificerx.core.ai.apex.agent.AgentTask
import com.waheed.artificerx.core.ai.apex.agent.AgentTaskGraph
import com.waheed.artificerx.core.ai.apex.agent.TaskNode
import com.waheed.artificerx.core.ai.apex.memory.CreativeMemoryStore
import com.waheed.artificerx.core.ai.apex.model.ModelRouter
import com.waheed.artificerx.core.ai.apex.tool.ToolRuntime
import com.waheed.artificerx.core.ai.apex.verify.VerificationEngine
import com.waheed.artificerx.core.ai.apex.vision.CanvasWorldModel
import com.waheed.artificerx.core.ai.apex.vision.VisualCritic

/** Provider-neutral agent kernel for the complete A-H runtime. */
class ApexAgentKernel(
    private val router: ModelRouter,
    private val tools: ToolRuntime,
    private val memory: CreativeMemoryStore,
    private val world: CanvasWorldModel,
    private val critic: VisualCritic,
    private val verifier: VerificationEngine,
) {
    fun plan(prompt: String, projectId: String, canvasRevision: Long): AgentTaskGraph {
        require(prompt.isNotBlank())
        val intent = prompt.trim()
        val nodes = mutableListOf<TaskNode>()
        nodes += TaskNode("inspect", "Inspect project and canvas state", emptySet(), 1.0)
        nodes += TaskNode("memory", "Retrieve relevant creative memory", setOf("inspect"), 0.7)
        if (intent.contains("draw", true) || intent.contains("edit", true) || intent.contains("change", true)) {
            nodes += TaskNode("plan_canvas", "Compile safe canvas operations", setOf("inspect", "memory"), 1.4)
            nodes += TaskNode("execute_canvas", "Execute approved canvas operations", setOf("plan_canvas"), 2.0)
            nodes += TaskNode("verify_visual", "Critique result against the requested intent", setOf("execute_canvas"), 1.5)
        } else if (intent.contains("research", true) || intent.contains("search", true)) {
            nodes += TaskNode("research", "Collect evidence and rank sources", setOf("inspect", "memory"), 2.0)
        } else {
            nodes += TaskNode("respond", "Generate a grounded response", setOf("inspect", "memory"), 1.0)
        }
        nodes += TaskNode("commit", "Commit verified artifacts and memory", nodes.map { it.id }.toSet(), 0.6)
        return AgentTaskGraph(AgentTask(projectId, canvasRevision, intent), nodes)
    }

    fun diagnostics(): Map<String, Any> = mapOf(
        "routerProviders" to router.providerCount(),
        "tools" to tools.count(),
        "memoryEntries" to memory.size(),
        "worldRevision" to world.revision(),
        "criticReady" to critic.ready(),
        "verifierReady" to verifier.ready(),
    )
}
