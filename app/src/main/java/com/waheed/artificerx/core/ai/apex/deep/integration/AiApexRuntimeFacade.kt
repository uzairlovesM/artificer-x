package com.waheed.artificerx.core.ai.apex.deep.integration

import com.waheed.artificerx.core.ai.apex.compat.ModelCapabilities
import com.waheed.artificerx.core.ai.apex.compat.UniversalModelProtocol
import com.waheed.artificerx.core.ai.apex.compat.UniversalRequest
import com.waheed.artificerx.core.ai.apex.compat.ProtocolRequest

class AiApexRuntimeFacade(
    private val kernel: UniversalAiRuntimeKernel = UniversalAiRuntimeKernel(),
    private val protocol: UniversalModelProtocol = UniversalModelProtocol(),
) {
    fun register(models: Iterable<RuntimeModel>) = models.forEach { kernel.registerModel(it) }

    fun plan(request: RuntimeRequest): RuntimeDecision = kernel.route(request)

    fun validate(request: UniversalRequest, model: ModelCapabilities): List<String> = protocol.validate(request, model)

    fun build(baseUrl: String, apiKey: String, model: ModelCapabilities, request: UniversalRequest): ProtocolRequest =
        protocol.buildRequest(baseUrl, apiKey, model, request)

    fun observe(modelId: String, success: Boolean, latencyMs: Long, tokens: Int = 0) =
        kernel.observeModel(modelId, success, latencyMs, tokens)

    fun remember(key: String, score: Double) = kernel.remember(key, score)

    fun recall(keys: Collection<String>, limit: Int = 8) = kernel.recall(keys, limit)

    fun runtimeEvents(limit: Int = 128) = kernel.events(limit)

    fun modelInventory() = kernel.models()
}
