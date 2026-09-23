package com.waheed.artificerx.core.image

import android.content.Context
import androidx.core.content.FileProvider
import com.waheed.artificerx.core.artifact.ArtifactStore
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.Reader
import java.io.IOException
import java.net.URI
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
        val providers = ImageProviderPolicy.rank(providerConfigRepository.configs.first())
        if (providers.isEmpty()) return@withContext Result.failure(IllegalStateException("No enabled image-capable network provider with an API key is configured."))
        require(prompt.length <= MAX_PROMPT_CHARS) { "Image prompt exceeds the safe ${MAX_PROMPT_CHARS}-character limit." }
        val model = (modelOverride?.takeIf { it.isNotBlank() } ?: "gpt-image-1").take(MAX_MODEL_CHARS)
        var lastError: Throwable? = null
        for (provider in providers) {
            val key = providerConfigRepository.rawKeyFor(provider.keyAlias) ?: continue
            val body = buildJsonObject {
                put("model", model)
                put("prompt", prompt)
                put("size", size)
            }.toString()
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
                    val body = response.body
                    if (body == null) {
                        lastError = IOException("${provider.displayName}: empty response body")
                        return@use
                    }
                    val payload = readResponseText(body, MAX_PROVIDER_RESPONSE_CHARS)
                    val item = json.parseToJsonElement(payload).jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject
                    if (item == null) {
                        lastError = IOException("${provider.displayName}: response contained no image data")
                        return@use
                    }
                    val downloaded = when {
                        item["b64_json"]?.jsonPrimitive?.contentOrNull != null -> {
                            val b64 = item["b64_json"]?.jsonPrimitive?.contentOrNull
                                ?: throw IOException("${provider.displayName}: empty base64 image payload")
                            val bytes = runCatching { Base64.getDecoder().decode(b64) }
                                .getOrElse { throw IOException("${provider.displayName}: invalid base64 image payload") }
                            ImagePayload(bytes, sniffMime(bytes))
                        }
                        item["url"]?.jsonPrimitive?.contentOrNull?.let { url -> download(url) }
                        else -> null
                    }
                    if (downloaded == null || downloaded.bytes.isEmpty()) {
                        lastError = IOException("${provider.displayName}: image response contained neither usable b64_json nor url")
                        return@use
                    }
                    require(downloaded.bytes.size.toLong() <= MAX_IMAGE_BYTES) {
                        "${provider.displayName}: generated image exceeds ${MAX_IMAGE_BYTES / (1024 * 1024)} MB limit"
                    }
                    val extension = extensionFor(downloaded.mimeType)
                    val fileName = "ai_image_${System.currentTimeMillis()}.$extension"
                    val artifact = artifactStore.writeFile(threadId, fileName, downloaded.bytes, downloaded.mimeType, "generate_image")
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

    private fun download(url: String): ImagePayload {
        val uri = URI(url.trim())
        require(uri.scheme.equals("https", true)) { "Image URL must use HTTPS." }
        require(uri.userInfo.isNullOrBlank()) { "Image URL credentials are not allowed." }
        require(!uri.host.isNullOrBlank()) { "Image URL host is missing." }
        client.newCall(Request.Builder().url(uri.toString()).get().build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Image URL HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty image body")
            val declared = body.contentType()?.toString()?.lowercase().orEmpty()
            val length = body.contentLength()
            if (length > MAX_IMAGE_BYTES) throw IOException("Image URL exceeds ${MAX_IMAGE_BYTES / (1024 * 1024)} MB limit")
            val output = ByteArrayOutputStream(minOf(if (length > 0) length.toInt() else 64 * 1024, MAX_IMAGE_BYTES.toInt()))
            body.byteStream().use { input ->
                val buffer = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_IMAGE_BYTES) throw IOException("Image URL exceeds ${MAX_IMAGE_BYTES / (1024 * 1024)} MB limit")
                    output.write(buffer, 0, read)
                }
            }
            val bytes = output.toByteArray()
            val mime = when {
                declared.contains("png") -> "image/png"
                declared.contains("jpeg") || declared.contains("jpg") -> "image/jpeg"
                declared.contains("webp") -> "image/webp"
                else -> sniffMime(bytes)
            }
            return ImagePayload(bytes, mime)
        }
    }

    private fun sniffMime(bytes: ByteArray): String = when {
        bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        )) -> "image/png"
        bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "image/jpeg"
        bytes.size >= 12 && bytes.copyOfRange(0, 4).contentEquals(byteArrayOf(0x52, 0x49, 0x46, 0x46)) &&
            bytes.copyOfRange(8, 12).contentEquals(byteArrayOf(0x57, 0x45, 0x42, 0x50)) -> "image/webp"
        else -> throw IOException("Unsupported image format")
    }

    private fun readResponseText(body: okhttp3.ResponseBody, maxChars: Int): String {
        val declaredLength = body.contentLength()
        if (declaredLength > maxChars.toLong()) throw IOException("Image provider response exceeds the safe response limit")
        val out = StringBuilder(minOf(maxChars, 64 * 1024))
        body.charStream().use { reader: Reader ->
            val buffer = CharArray(16 * 1024)
            while (out.length < maxChars) {
                val read = reader.read(buffer, 0, minOf(buffer.size, maxChars - out.length))
                if (read < 0) break
                out.append(buffer, 0, read)
            }
            if (reader.read() >= 0) throw IOException("Image provider response exceeds the safe response limit")
        }
        return out.toString()
    }

    private fun extensionFor(mime: String): String = when (mime.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/webp" -> "webp"
        else -> "png"
    }

    private data class ImagePayload(val bytes: ByteArray, val mimeType: String)

    private companion object {
        const val MAX_IMAGE_BYTES = 25L * 1024L * 1024L
        const val MAX_PROVIDER_RESPONSE_CHARS = 40 * 1024 * 1024
        const val MAX_PROMPT_CHARS = 16_000
        const val MAX_MODEL_CHARS = 200
    }
}
