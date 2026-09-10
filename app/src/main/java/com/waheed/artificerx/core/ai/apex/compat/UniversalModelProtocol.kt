package com.waheed.artificerx.core.ai.apex.compat

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

enum class ProtocolFamily { OPENAI_CHAT, OPENAI_RESPONSES, GEMINI_CONTENT, ANTHROPIC_MESSAGES, LOCAL_LLAMA, GENERIC_JSON }
enum class InputKind { TEXT, IMAGE, AUDIO, VIDEO, PDF, FILE }
enum class OutputKind { TEXT, JSON, TOOL_CALLS, IMAGE, AUDIO }

data class ModelCapabilities(
    val id: String,
    val protocol: ProtocolFamily,
    val contextWindow: Long,
    val maxOutputTokens: Long?,
    val inputs: Set<InputKind>,
    val outputs: Set<OutputKind>,
    val tools: Boolean,
    val streaming: Boolean,
    val structuredOutput: Boolean,
    val reasoning: Boolean,
    val local: Boolean,
) {
    fun supports(input: InputKind) = input in inputs
    fun supports(output: OutputKind) = output in outputs
}

data class UniversalTurn(
    val role: String,
    val text: String? = null,
    val media: List<MediaPart> = emptyList(),
    val toolCalls: List<UniversalToolCall> = emptyList(),
    val toolResultFor: String? = null,
)

data class MediaPart(val kind: InputKind, val uri: String, val mimeType: String? = null)
data class UniversalToolCall(val id: String, val name: String, val argumentsJson: String)
data class UniversalToolResult(val id: String, val outputJson: String, val isError: Boolean = false)

data class UniversalRequest(
    val model: String,
    val system: String? = null,
    val turns: List<UniversalTurn>,
    val tools: List<UniversalToolDefinition> = emptyList(),
    val temperature: Double? = null,
    val maxOutputTokens: Long? = null,
    val stream: Boolean = true,
    val responseFormat: String? = null,
    val reasoningEffort: String? = null,
)

data class UniversalToolDefinition(val name: String, val description: String, val schemaJson: String)

data class ProtocolRequest(
    val family: ProtocolFamily,
    val url: String,
    val headers: Map<String, String>,
    val body: String,
    val model: String,
)

data class StreamPiece(
    val sequence: Long,
    val text: String? = null,
    val toolCall: UniversalToolCall? = null,
    val finishReason: String? = null,
)

data class StreamAssembly(val text: String, val tools: List<UniversalToolCall>, val finishReason: String?, val digest: String)

class UniversalModelProtocol {
    fun chooseProtocol(model: ModelCapabilities): ProtocolFamily = model.protocol

    fun buildRequest(baseUrl: String, apiKey: String, capabilities: ModelCapabilities, request: UniversalRequest): ProtocolRequest {
        val url = endpoint(baseUrl, capabilities.protocol, request.model)
        val headers = linkedMapOf("Authorization" to "Bearer $apiKey", "Content-Type" to "application/json")
        val body = when (capabilities.protocol) {
            ProtocolFamily.OPENAI_CHAT, ProtocolFamily.LOCAL_LLAMA -> openAiChat(request)
            ProtocolFamily.OPENAI_RESPONSES -> openAiResponses(request)
            ProtocolFamily.GEMINI_CONTENT -> gemini(request)
            ProtocolFamily.ANTHROPIC_MESSAGES -> anthropic(request)
            ProtocolFamily.GENERIC_JSON -> generic(request)
        }
        if (capabilities.protocol == ProtocolFamily.ANTHROPIC_MESSAGES) headers.remove("Authorization")
        if (capabilities.protocol == ProtocolFamily.ANTHROPIC_MESSAGES) headers["x-api-key"] = apiKey
        return ProtocolRequest(capabilities.protocol, url, headers, body, request.model)
    }

    fun validate(request: UniversalRequest, model: ModelCapabilities): List<String> = buildList {
        if (request.model.isBlank()) add("model-empty")
        if (request.turns.isEmpty()) add("conversation-empty")
        if (request.turns.any { it.role !in setOf("system", "user", "assistant", "tool") }) add("invalid-role")
        request.turns.flatMap { it.media }.forEach { media -> if (!model.supports(media.kind)) add("unsupported-input:${media.kind}") }
        if (request.tools.isNotEmpty() && !model.tools) add("tools-unsupported")
        if (request.stream && !model.streaming) add("streaming-unsupported")
        if (request.responseFormat != null && !model.structuredOutput) add("structured-output-unsupported")
        request.maxOutputTokens?.let { limit ->
            if (limit <= 0) add("max-output-nonpositive")
            model.maxOutputTokens?.let { maximum -> if (limit > maximum) add("max-output-exceeds-model") }
        }
        if (request.reasoningEffort != null && !model.reasoning) add("reasoning-effort-unsupported")
        val promptEstimate = estimateTokens(request)
        if (promptEstimate >= model.contextWindow) add("context-window-exceeded:$promptEstimate/${model.contextWindow}")
    }

    fun estimateTokens(request: UniversalRequest): Long {
        val chars = buildString {
            append(request.system.orEmpty())
            request.turns.forEach { turn ->
                append(turn.role).append(':').append(turn.text.orEmpty())
                turn.media.forEach { append(it.kind.name).append(it.uri) }
                turn.toolCalls.forEach { append(it.name).append(it.argumentsJson) }
            }
            request.tools.forEach { append(it.name).append(it.description).append(it.schemaJson) }
        }.length
        return max(1L, chars / 4L)
    }

    fun assemble(pieces: List<StreamPiece>): StreamAssembly {
        val ordered = pieces.sortedBy { it.sequence }
        val text = buildString { ordered.forEach { append(it.text.orEmpty()) } }
        val tools = ordered.mapNotNull { it.toolCall }.groupBy { it.id }.values.map { parts ->
            val first = parts.first()
            val args = parts.joinToString(separator = "") { it.argumentsJson }
            first.copy(argumentsJson = args)
        }
        val finish = ordered.lastOrNull()?.finishReason
        val digest = sha256(text + tools.joinToString { it.id + it.name + it.argumentsJson })
        return StreamAssembly(text, tools, finish, digest)
    }

    private fun endpoint(base: String, protocol: ProtocolFamily, model: String): String {
        val trimmed = base.trimEnd('/')
        return when (protocol) {
            ProtocolFamily.OPENAI_CHAT -> if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/chat/completions"
            ProtocolFamily.OPENAI_RESPONSES -> if (trimmed.endsWith("/responses")) trimmed else "$trimmed/responses"
            ProtocolFamily.GEMINI_CONTENT -> "$trimmed/v1beta/models/$model:generateContent"
            ProtocolFamily.ANTHROPIC_MESSAGES -> if (trimmed.endsWith("/messages")) trimmed else "$trimmed/v1/messages"
            ProtocolFamily.LOCAL_LLAMA -> if (trimmed.endsWith("/chat/completions")) trimmed else "$trimmed/v1/chat/completions"
            ProtocolFamily.GENERIC_JSON -> trimmed
        }
    }

    private fun openAiChat(r: UniversalRequest): String = jsonObject(
        "model" to quote(r.model),
        "messages" to messagesJson(r.system, r.turns),
        "tools" to if (r.tools.isEmpty()) "null" else r.tools.joinToString(prefix = "[", postfix = "]") { tool -> "{\"type\":\"function\",\"function\":{\"name\":${quote(tool.name)},\"description\":${quote(tool.description)},\"parameters\":${tool.schemaJson}}}" },
        "tool_choice" to if (r.tools.isEmpty()) "null" else quote("auto"),
        "temperature" to (r.temperature ?: 0.7).coerceIn(0.0, 2.0).toString(),
        "max_tokens" to (r.maxOutputTokens?.coerceAtLeast(1L)?.toString() ?: "null"),
        "stream" to r.stream.toString(),
        "reasoning_effort" to (r.reasoningEffort?.let(::quote) ?: "null"),
    )

    private fun openAiResponses(r: UniversalRequest): String = jsonObject(
        "model" to quote(r.model),
        "input" to quote(r.turns.joinToString("\n") { "${it.role}: ${it.text.orEmpty()}" }),
        "stream" to r.stream.toString(),
        "temperature" to (r.temperature ?: 0.7).coerceIn(0.0, 2.0).toString(),
        "max_output_tokens" to (r.maxOutputTokens?.coerceAtLeast(1L)?.toString() ?: "null"),
    )

    private fun gemini(r: UniversalRequest): String = jsonObject(
        "contents" to r.turns.filter { it.role != "system" }.joinToString(prefix = "[", postfix = "]") { turn -> "{\"role\":${quote(if (turn.role == "assistant") "model" else "user")},\"parts\":[{\"text\":${quote(turn.text.orEmpty())}}]}" },
        "systemInstruction" to (r.system?.let { "{\"parts\":[{\"text\":${quote(it)}}]}" } ?: "null"),
        "tools" to if (r.tools.isEmpty()) "null" else "[{\"functionDeclarations\":[${r.tools.joinToString { "{\"name\":${quote(it.name)},\"description\":${quote(it.description)},\"parameters\":${it.schemaJson}}" }}]",
    )

    private fun anthropic(r: UniversalRequest): String = jsonObject(
        "model" to quote(r.model),
        "system" to (r.system?.let(::quote) ?: "null"),
        "messages" to r.turns.filter { it.role != "system" }.joinToString(prefix = "[", postfix = "]") { turn -> "{\"role\":${quote(if (turn.role == "assistant") "assistant" else "user")},\"content\":${quote(turn.text.orEmpty())}}" },
        "max_tokens" to (r.maxOutputTokens?.coerceAtLeast(1L)?.toString() ?: "1024"),
        "temperature" to (r.temperature ?: 0.7).coerceIn(0.0, 1.0).toString(),
        "stream" to r.stream.toString(),
    )

    private fun generic(r: UniversalRequest): String = jsonObject(
        "model" to quote(r.model),
        "prompt" to quote(r.turns.joinToString("\n") { "${it.role}: ${it.text.orEmpty()}" }),
    )

    private fun messagesJson(system: String?, turns: List<UniversalTurn>): String = buildString {
        append('[')
        var first = true
        fun add(value: String) { if (!first) append(','); first = false; append(value) }
        system?.let { add("{\"role\":\"system\",\"content\":${quote(it)}}") }
        turns.forEach { turn ->
            val content = when {
                turn.media.isNotEmpty() -> {
                    val parts = mutableListOf<String>()
                    turn.text?.let { parts += "{\"type\":\"text\",\"text\":${quote(it)}}" }
                    turn.media.forEach { media ->
                        parts += "{\"type\":\"image_url\",\"image_url\":{\"url\":${quote(media.uri)}}}"
                    }
                    "[${parts.joinToString(",")}]"
                }
                else -> quote(turn.text.orEmpty())
            }
            add("{\"role\":${quote(turn.role)},\"content\":$content}")
        }
        append(']')
    }

    private fun jsonObject(vararg fields: Pair<String, String>): String = fields.joinToString(prefix = "{", postfix = "}") { "${quote(it.first)}:${it.second}" }
    private fun quote(value: String): String = buildString { append('"'); value.forEach { ch -> when (ch) { '"' -> append("\\\""); '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> if (ch.code < 32) append("\\u%04x".format(ch.code)) else append(ch) } }; append('"') }
    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}

class ModelCompatibilityMatrix {
    fun score(model: ModelCapabilities, request: UniversalRequest): Double {
        val failures = UniversalModelProtocol().validate(request, model)
        var score = model.contextWindow.toDouble().coerceAtMost(1_000_000.0) / 1_000_000.0
        score += model.healthHeuristic()
        if (request.model.equals(model.id, ignoreCase = true)) score += 1.0
        if (request.needsVision() && model.supports(InputKind.IMAGE)) score += 0.5
        if (request.tools.isNotEmpty() && model.tools) score += 0.5
        if (request.responseFormat != null && model.structuredOutput) score += 0.4
        if (request.stream && model.streaming) score += 0.2
        score -= failures.size * 1.2
        return score
    }
    private fun ModelCapabilities.healthHeuristic(): Double = (if (local) 0.2 else 0.1) + (if (reasoning) 0.1 else 0.0)
    private fun UniversalRequest.needsVision(): Boolean = turns.any { it.media.any { media -> media.kind == InputKind.IMAGE } }
}
