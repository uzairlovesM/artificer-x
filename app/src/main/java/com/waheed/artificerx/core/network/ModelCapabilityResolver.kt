package com.waheed.artificerx.core.network
import com.waheed.artificerx.domain.model.AiProviderConfig

/**
 * Resolves model capabilities from provider metadata first, then uses a
 * deliberately labelled heuristic fallback. This prevents a model named
 * `qwen-vl` from being the only vision model the UI recognizes while also
 * avoiding the dangerous claim that an undocumented model is definitely
 * vision-capable.
 */
object ModelCapabilityResolver {
    private val visionNamePatterns = listOf(
        "vision", "-vl", "_vl", "/vl", "llava", "pixtral", "gemini",
        "gpt-4o", "gpt-4.1", "gpt-4.5", "gpt-5", "gpt-6", "claude",
        "mistral-small-3.1", "mistral-small-3.2", "mistral-medium-3",
        "pixtral", "pixtral-large", "mistral-large-2", "internvl", "internlm",
        "phi-3.5-vision", "phi-4-multimodal", "molmo", "gemma-3", "llama-4",
        "kimi-vl", "doubao-vision", "grok-vision",
        "qwen2.5-vl", "qwen3-vl", "qwen3.5-vl", "qwen-vl",
    )
    private val reasoningNamePatterns = listOf(
        "reason", "reasoning", "deepseek-r1", "deepseek-v3.1", "o1", "o3", "o4",
        "gpt-5", "gpt-6", "qwen3", "gemini-2.5", "gemini-3", "claude",
    )
    private val toolParameterPatterns = listOf(
        "tools", "tool_choice", "function_calling", "parallel_tool_calls", "response_format", "structured_outputs",
    )
    private val reasoningParameterPatterns = listOf(
        "reasoning", "reasoning_effort", "thinking", "thinking_level",
    )

    internal fun fromOpenRouter(model: ModelDto): RemoteModelInfo {
        val input = (model.architecture?.inputModalities.orEmpty() + model.inputModalities + model.modalities).map { it.lowercase() }.distinct()
        val output = (model.architecture?.outputModalities.orEmpty() + model.outputModalities + model.modalities).map { it.lowercase() }.distinct()
        val modalityHint = model.architecture?.modality?.lowercase().orEmpty()
        val params = model.supportedParameters.map { it.lowercase() }
        val id = model.id.lowercase()
        val visionConfirmed = input.any { it == "image" || it == "image_url" || it == "vision" || it == "multimodal" } || modalityHint.contains("image") || modalityHint.contains("vision")
        val visionInferred = !visionConfirmed && visionNamePatterns.any(id::contains)
        val toolConfirmed = params.any { candidate -> toolParameterPatterns.any(candidate::contains) }
        val toolInferred = !toolConfirmed && listOf("tool", "function", "mcp", "agent").any(id::contains)
        val reasoningConfirmed = params.any { candidate -> reasoningParameterPatterns.any(candidate::contains) }
        val reasoningInferred = !reasoningConfirmed && reasoningNamePatterns.any(id::contains)
        return RemoteModelInfo(
            id = model.id,
            displayName = model.name,
            description = model.description,
            supportsVision = visionConfirmed || visionInferred,
            visionEvidence = when {
                visionConfirmed -> ModelCapabilityEvidence.CONFIRMED
                visionInferred -> ModelCapabilityEvidence.INFERRED
                else -> ModelCapabilityEvidence.UNKNOWN
            },
            supportsToolCalling = toolConfirmed || toolInferred,
            toolCallingEvidence = when {
                toolConfirmed -> ModelCapabilityEvidence.CONFIRMED
                toolInferred -> ModelCapabilityEvidence.INFERRED
                else -> ModelCapabilityEvidence.UNKNOWN
            },
            supportsReasoning = reasoningConfirmed || reasoningInferred,
            reasoningEvidence = when {
                reasoningConfirmed -> ModelCapabilityEvidence.CONFIRMED
                reasoningInferred -> ModelCapabilityEvidence.INFERRED
                else -> ModelCapabilityEvidence.UNKNOWN
            },
            contextWindow = model.contextLength,
            inputModalities = input,
            outputModalities = output,
            supportedParameters = params,
        )
    }

    /** Effective runtime fallback for older stored providers that do not
     * have per-model capability metadata yet. */
    fun effectiveVision(provider: AiProviderConfig): Boolean {
        val modelId = provider.defaultModelId ?: return provider.supportsVision
        if (provider.visionModelIds.any { it.equals(modelId, ignoreCase = true) }) return true
        val id = modelId.lowercase()
        return if (visionNamePatterns.any(id::contains)) true else provider.supportsVision
    }

    fun evidenceLabel(evidence: ModelCapabilityEvidence): String = when (evidence) {
        ModelCapabilityEvidence.CONFIRMED -> "Vision confirmed"
        ModelCapabilityEvidence.INFERRED -> "Vision likely"
        ModelCapabilityEvidence.UNKNOWN -> "Vision unknown"
    }
}
