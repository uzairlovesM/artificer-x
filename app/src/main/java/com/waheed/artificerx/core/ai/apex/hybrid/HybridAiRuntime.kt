package com.waheed.artificerx.core.ai.apex.hybrid

import com.waheed.artificerx.core.ai.apex.kernel.ApexAgentKernel
import com.waheed.artificerx.core.ai.apex.generation.GenerationGateway
import com.waheed.artificerx.core.ai.apex.model.ModelMode
import com.waheed.artificerx.core.ai.apex.model.ModelRouter

data class HybridRequest(val prompt: String, val mode: ModelMode, val vision: Boolean, val tools: Boolean, val projectId: String, val canvasRevision: Long)
data class HybridDecision(val selectedModel: String?, val planNodes: Int, val generationAvailable: Boolean, val reasons: List<String>)
class HybridAiRuntime(private val router: ModelRouter, private val kernel: ApexAgentKernel, private val generation: GenerationGateway) {
    fun decide(request: HybridRequest): HybridDecision {
        val selection = router.select(request.mode, request.vision, request.tools)
        val graph = kernel.plan(request.prompt, request.projectId, request.canvasRevision)
        return HybridDecision(selection?.endpoint?.id, graph.nodes.size, true, selection?.reasons ?: listOf("no_capable_model"))
    }
}
