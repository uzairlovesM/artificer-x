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

data class RemoteModelInfo(
    val id: String,
    val supportsVision: Boolean,
    val supportsToolCalling: Boolean,
    val contextWindow: Int?,
)

@Serializable
internal data class ModelListResponseDto(
    val data: List<ModelDto> = emptyList(),
)

@Serializable
internal data class ModelDto(
    val id: String,
)
