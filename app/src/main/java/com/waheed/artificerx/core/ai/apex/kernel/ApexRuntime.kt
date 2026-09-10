package com.waheed.artificerx.core.ai.apex.kernel

import com.waheed.artificerx.core.ai.apex.ContextEngine
import com.waheed.artificerx.core.ai.apex.generation.DeterministicGenerationAdapter
import com.waheed.artificerx.core.ai.apex.generation.GenerationGateway
import com.waheed.artificerx.core.ai.apex.index.ApexCapability
import com.waheed.artificerx.core.ai.apex.index.ApexCapabilityRegistry
import com.waheed.artificerx.core.ai.apex.memory.CreativeMemoryStore
import com.waheed.artificerx.core.ai.apex.model.ModelEndpoint
import com.waheed.artificerx.core.ai.apex.model.ModelRouter
import com.waheed.artificerx.core.ai.apex.tool.ToolRuntime
import com.waheed.artificerx.core.ai.apex.verify.VerificationEngine
import com.waheed.artificerx.core.ai.apex.vision.CanvasWorldModel
import com.waheed.artificerx.core.ai.apex.vision.VisualCritic

data class ApexRuntimeBundle(val kernel: ApexAgentKernel, val context: ContextEngine, val memory: CreativeMemoryStore, val router: ModelRouter, val tools: ToolRuntime, val generation: GenerationGateway, val capabilities: ApexCapabilityRegistry)
object ApexRuntimeFactory {
    fun create(): ApexRuntimeBundle {
        val memory = CreativeMemoryStore()
        val router = ModelRouter(mutableListOf(ModelEndpoint("deterministic-local", true, false, true, 8_192)))
        val tools = ToolRuntime()
        val generation = GenerationGateway().also { it.register(DeterministicGenerationAdapter()) }
        val world = CanvasWorldModel()
        val critic = VisualCritic()
        val verifier = VerificationEngine()
        val kernel = ApexAgentKernel(router, tools, memory, world, critic, verifier)
        val capabilities = ApexCapabilityRegistry(tools).also {
            it.registerCoreTools()
            ApexCapability.values().forEach(it::enable)
        }
        return ApexRuntimeBundle(kernel, ContextEngine(), memory, router, tools, generation, capabilities)
    }
}
