package com.waheed.artificerx.core.ai.apex.index

import com.waheed.artificerx.core.ai.apex.tool.ToolRuntime
import com.waheed.artificerx.core.ai.apex.tool.ToolSpec

enum class ApexCapability { AGENT_OS, CANVAS_INTELLIGENCE, VISUAL_CRITIC, MULTI_AGENT, CREATIVE_MEMORY, GENERATION, AUTONOMOUS_WORKFLOW, HYBRID_RUNTIME }
class ApexCapabilityRegistry(private val tools: ToolRuntime) {
    private val capabilities = linkedSetOf<ApexCapability>()
    fun enable(capability: ApexCapability) { capabilities += capability }
    fun enabled(): Set<ApexCapability> = capabilities.toSet()
    fun registerCoreTools() {
        listOf(
            ToolSpec("canvas.inspect", 1, false, true),
            ToolSpec("canvas.propose", 1, false, true),
            ToolSpec("canvas.verify", 1, false, true),
            ToolSpec("vision.diff", 1, false, true),
            ToolSpec("memory.recall", 1, false, true),
            ToolSpec("memory.remember", 1, false, false),
            ToolSpec("generation.request", 1, false, false),
            ToolSpec("workflow.run", 1, false, false),
        ).forEach(tools::register)
    }
}
