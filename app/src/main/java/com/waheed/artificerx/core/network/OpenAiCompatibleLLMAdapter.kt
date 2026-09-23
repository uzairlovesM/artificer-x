package com.waheed.artificerx.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete LLMAdapter for any OpenAI-compatible provider — covers Groq,
 * OpenRouter, and the custom-router case (Section 196) with a single
 * implementation. Cloudflare Workers AI is also OpenAI-compatible at
 * /v1/chat/completions per Section 205, with the account ID folded into
 * the base URL path rather than a header, handled by callers passing
 * the full pre-built baseUrl.
 *
 * Section 191 Reliability Engineering: every failure mode (401/403 →
 * invalid key, 429 → rate limited, IOException → unreachable, anything
 * else → unknown) maps to a distinct ConnectionTestResult so the UI can
 * show an actionable message instead of a generic "something went
 * wrong."
 */
@Singleton
class OpenAiCompatibleLLMAdapter
    @Inject
    constructor() : LLMAdapter {
        private val client =
            OkHttpClient
                .Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

        private val json = Json { ignoreUnknownKeys = true }

        override suspend fun testConnection(
            baseUrl: String,
            apiKey: String,
            accountId: String?,
        ): ConnectionTestResult =
            withContext(Dispatchers.IO) {
                val startedAt = System.currentTimeMillis()
                val url = "${baseUrl.trimEnd('/')}/models"

                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Authorization", "Bearer $apiKey")
                        .get()
                        .build()

                try {
                    client.newCall(request).execute().use { response ->
                        val latency = System.currentTimeMillis() - startedAt
                        when (response.code) {
                            in 200..299 -> {
                                val body = readResponseText(response, MAX_RESPONSE_CHARS)
                                val parsed =
                                    runCatching {
                                        json.decodeFromString<ModelListResponseDto>(body)
                                    }.getOrNull()
                                ConnectionTestResult.Success(
                                    modelCount = parsed?.data?.size ?: 0,
                                    latencyMillis = latency,
                                )
                            }
                            401, 403 ->
                                ConnectionTestResult.InvalidKey(
                                    "The API key was rejected by the provider (HTTP ${response.code})",
                                )
                            429 -> {
                                val retryAfter = response.header("Retry-After")?.toIntOrNull()
                                ConnectionTestResult.RateLimited(retryAfter)
                            }
                            else ->
                                ConnectionTestResult.UnknownError(
                                    "Unexpected response (HTTP ${response.code})",
                                )
                        }
                    }
                } catch (e: IOException) {
                    ConnectionTestResult.Unreachable(e.message ?: "Network unreachable")
                } catch (e: SerializationException) {
                    ConnectionTestResult.UnknownError(e.message ?: "Malformed response from provider")
                }
            }

        override suspend fun listModels(
            baseUrl: String,
            apiKey: String,
            accountId: String?,
        ): Result<List<RemoteModelInfo>> =
            withContext(Dispatchers.IO) {
                val url = "${baseUrl.trimEnd('/')}/models"
                val request =
                    Request
                        .Builder()
                        .url(url)
                        .header("Authorization", "Bearer $apiKey")
                        .get()
                        .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            return@withContext Result.failure(IOException("HTTP ${response.code}"))
                        }
                        val body = readResponseText(response, MAX_RESPONSE_CHARS)
                        val parsed = json.decodeFromString<ModelListResponseDto>(body)
                        var models = parsed.data.map { dto -> ModelCapabilityResolver.fromOpenRouter(dto) }

                        // OpenRouter documents architecture.input_modalities as the source of truth,
                        // and also exposes a first-class `input_modalities=image` catalog filter.
                        // Cross-check that filtered catalog so a provider response that omits modality
                        // fields on individual model records does not silently turn a real VLM into a
                        // text-only model inside Artificer-X. This secondary request is best-effort and
                        // only runs against OpenRouter URLs; other OpenAI-compatible providers keep their
                        // normal single /models request.
                        if (baseUrl.contains("openrouter.ai", ignoreCase = true) && models.isNotEmpty()) {
                            runCatching {
                                val imageUrl = response.request.url.newBuilder()
                                    .addQueryParameter("input_modalities", "image")
                                    .build()
                                val imageRequest = request.newBuilder().url(imageUrl).build()
                                client.newCall(imageRequest).execute().use { imageResponse ->
                                    if (imageResponse.isSuccessful) {
                                        val imageBody = readResponseText(imageResponse, MAX_RESPONSE_CHARS)
                                        val imageParsed = json.decodeFromString<ModelListResponseDto>(imageBody)
                                        val visionIds = imageParsed.data.map { it.id }.toSet()
                                        if (visionIds.isNotEmpty()) {
                                            models = models.map { info ->
                                                if (info.id in visionIds && !info.supportsVision) {
                                                    info.copy(
                                                        supportsVision = true,
                                                        visionEvidence = ModelCapabilityEvidence.CONFIRMED,
                                                        inputModalities = (info.inputModalities + "image").distinct(),
                                                    )
                                                } else {
                                                    info
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Result.success(models)
                    }
                } catch (e: IOException) {
                    Result.failure(e)
                } catch (e: SerializationException) {
                    Result.failure(e)
                }
            }
        private fun readResponseText(response: okhttp3.Response, maxChars: Int): String {
            val body = response.body ?: return ""
            if (body.contentLength() > maxChars.toLong() * 2L) return ""
            return body.charStream().buffered().use { reader ->
                val out = StringBuilder(minOf(maxChars, 16 * 1024))
                val buffer = CharArray(8 * 1024)
                while (out.length < maxChars) {
                    val n = reader.read(buffer, 0, minOf(buffer.size, maxChars - out.length))
                    if (n < 0) break
                    out.append(buffer, 0, n)
                }
                out.toString()
            }
        }


        private companion object {
            const val MAX_RESPONSE_CHARS = 1_000_000
        }

    }
