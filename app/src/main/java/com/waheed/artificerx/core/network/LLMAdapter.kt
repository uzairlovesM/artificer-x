package com.waheed.artificerx.core.network

import kotlinx.serialization.Serializable

/**
 * Section 74/165 BackendAdapter pattern applied to Reasoning Brain
 * providers. Every provider (Groq, OpenRouter, Cloudflare Workers AI,
 * Custom) is OpenAI-compatible at the /chat/completions surface, so one
 * adapter interface + one OkHttp client covers all of them — provider
 * differences are just base URL, auth header shape, and model-list
 * endpoint, isolated in OpenAiCompatibleLLMAdapter below.
 */
interface LLMAdapter {
    suspend fun testConnection(
        baseUrl: String,
        apiKey: String,
        accountId: String? = null,
    ): ConnectionTestResult

    suspend fun listModels(
        baseUrl: String,
        apiKey: String,
        accountId: String? = null,
    ): Result<List<RemoteModelInfo>>
}

sealed class ConnectionTestResult {
    data class Success(
        val modelCount: Int,
        val latencyMillis: Long,
    ) : ConnectionTestResult()

    data class InvalidKey(
        val message: String,
    ) : ConnectionTestResult()

    data class RateLimited(
        val retryAfterSeconds: Int?,
    ) : ConnectionTestResult()

    data class Unreachable(
        val message: String,
    ) : ConnectionTestResult()

    data class UnknownError(
        val message: String,
    ) : ConnectionTestResult()
}

enum class ModelCapabilityEvidence {
    CONFIRMED,
    INFERRED,
    UNKNOWN,
}

data class RemoteModelInfo(
    val id: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportsVision: Boolean,
    val visionEvidence: ModelCapabilityEvidence = ModelCapabilityEvidence.UNKNOWN,
    val supportsToolCalling: Boolean,
    val toolCallingEvidence: ModelCapabilityEvidence = ModelCapabilityEvidence.UNKNOWN,
    val supportsReasoning: Boolean = false,
    val reasoningEvidence: ModelCapabilityEvidence = ModelCapabilityEvidence.UNKNOWN,
    val contextWindow: Int?,
    val inputModalities: List<String> = emptyList(),
    val outputModalities: List<String> = emptyList(),
    val supportedParameters: List<String> = emptyList(),
)

@Serializable
internal data class ModelListResponseDto(
    val data: List<ModelDto> = emptyList(),
)

@Serializable
internal data class ModelArchitectureDto(
    @kotlinx.serialization.SerialName("input_modalities") val inputModalities: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("output_modalities") val outputModalities: List<String> = emptyList(),
    val modality: String? = null,
)

@Serializable
internal data class ModelDto(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val architecture: ModelArchitectureDto? = null,
    @kotlinx.serialization.SerialName("input_modalities") val inputModalities: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("output_modalities") val outputModalities: List<String> = emptyList(),
    val modalities: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("supported_parameters") val supportedParameters: List<String> = emptyList(),
    @kotlinx.serialization.SerialName("context_length") val contextLength: Int? = null,
)
