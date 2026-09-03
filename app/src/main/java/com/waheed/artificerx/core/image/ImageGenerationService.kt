package com.waheed.artificerx.core.image

import android.content.Context
import androidx.core.content.FileProvider
import com.waheed.artificerx.core.artifact.ArtifactStore
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class GeneratedImageArtifact(val fileName: String, val uri: android.net.Uri, val path: String, val sizeBytes: Long)

@Singleton
class ImageGenerationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerConfigRepository: ProviderConfigRepository,
    private val artifactStore: ArtifactStore,
) {
    private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generate(threadId: String, prompt: String, size: String = "1024x1024", modelOverride: String? = null): Result<GeneratedImageArtifact> = withContext(Dispatchers.IO) {
        val providers = ImageProviderPolicy.rank(kotlinx.coroutines.flow.first(providerConfigRepository.configs))
        if (providers.isEmpty()) return@withContext Result.failure(IllegalStateException("No enabled image-capable network provider with an API key is configured."))
        val model = modelOverride?.takeIf { it.isNotBlank() } ?: "gpt-image-1"
        var lastError: Throwable? = null
        for (provider in providers) {
            val key = providerConfigRepository.rawKeyFor(provider.keyAlias) ?: continue
            val body = """{"model":"${escape(model)}","prompt":"${escape(prompt)}","size":"${escape(size)}"}"""
            val request = Request.Builder()
                .url("${provider.baseUrl.trimEnd('/')}/images/generations")
                .header("Authorization", "Bearer $key")
                .header("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastError = IOException("${provider.displayName}: HTTP ${response.code}")
                        return@use
                    }
                    val payload = response.body?.string().orEmpty()
                    val item = json.parseToJsonElement(payload).jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject
                    if (item == null) {
                        lastError = IOException("${provider.displayName}: response contained no image data")
                        return@use
                    }
                    val bytes = when {
                        item["b64_json"]?.jsonPrimitive?.contentOrNull != null -> Base64.getDecoder().decode(item["b64_json"]!!.jsonPrimitive.content)
                        item["url"]?.jsonPrimitive?.contentOrNull != null -> download(item["url"]!!.jsonPrimitive.content)
                        else -> null
                    }
                    if (bytes == null || bytes.isEmpty()) {
                        lastError = IOException("${provider.displayName}: image response contained neither usable b64_json nor url")
                        return@use
                    }
                    val fileName = "ai_image_${System.currentTimeMillis()}.png"
                    val artifact = artifactStore.writeFile(threadId, fileName, bytes, "image/png", "generate_image")
                    val file = java.io.File(artifact.path)
                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                    return@withContext Result.success(GeneratedImageArtifact(fileName, uri, artifact.path, artifact.sizeBytes))
                }
            } catch (t: Throwable) {
                lastError = t
            }
        }
        Result.failure(lastError ?: IOException("All image providers failed."))
    }

    private fun download(url: String): ByteArray {
        client.newCall(Request.Builder().url(url).get().build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Image URL HTTP ${response.code}")
            return response.body?.bytes() ?: throw IOException("Empty image body")
        }
    }

    private fun escape(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
}
