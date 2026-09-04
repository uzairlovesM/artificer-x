package com.waheed.artificerx.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val tools: List<ToolDefinitionDto>? = null,
    @SerialName("tool_choice") val toolChoice: String? = "auto",
    val temperature: Double = 0.7,
    /** Null means: do not send an artificial application-side output ceiling.
     * The provider/model remains the ultimate authority for its own limit. */
    @SerialName("max_tokens") val maxTokens: Int? = null,
    val stream: Boolean = false,
    // v0.4.30 Deep Studio mode: OpenAI-compatible reasoning-effort hint
    // ("low"/"medium"/"high"), forwarded only when the active
    // QualityPreset is DEEP_STUDIO (see AgentSettings.reasoningEffort).
    // Left null for every other preset/provider combination so models
    // that don't recognize the field never receive it.
    @SerialName("reasoning_effort") val reasoningEffort: String? = null,
)

/**
 * OpenAI-compatible chat message. The `content` field is polymorphic
 * in the real API: a plain string for "system"/"assistant"/"tool"
 * roles, or an array of ContentPartDto only for "user" messages that
 * mix text and image_url (vision input). Serializing an array for
 * every role — the earlier version of this DTO — causes Groq/
 * OpenRouter to reject assistant and tool messages with 400 Bad
 * Request, since their schema validation expects a bare string there.
 *
 * contentText / contentParts are mutually exclusive by convention:
 * set contentText for system/assistant/tool messages, contentParts
 * for user messages with an attached image. A custom serializer
 * resolves which one to emit.
 */
@Serializable(with = ChatMessageDtoSerializer::class)
data class ChatMessageDto(
    val role: String,
    val contentText: String? = null,
    val contentParts: List<ContentPartDto>? = null,
    val toolCalls: List<ToolCallDto>? = null,
    val toolCallId: String? = null,
    val name: String? = null,
)

object ChatMessageDtoSerializer : kotlinx.serialization.KSerializer<ChatMessageDto> {
    override val descriptor: kotlinx.serialization.descriptors.SerialDescriptor =
        kotlinx.serialization.descriptors.buildClassSerialDescriptor("ChatMessageDto")

    override fun serialize(
        encoder: kotlinx.serialization.encoding.Encoder,
        value: ChatMessageDto,
    ) {
        val jsonEncoder =
            encoder as? kotlinx.serialization.json.JsonEncoder
                ?: error("ChatMessageDtoSerializer only supports JSON")

        val map = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>()
        map["role"] = kotlinx.serialization.json.JsonPrimitive(value.role)

        when {
            value.contentParts != null -> {
                map["content"] =
                    jsonEncoder.json.encodeToJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(ContentPartDto.serializer()),
                        value.contentParts,
                    )
            }
            value.contentText != null -> {
                map["content"] = kotlinx.serialization.json.JsonPrimitive(value.contentText)
            }
            else -> {
                map["content"] = kotlinx.serialization.json.JsonNull
            }
        }

        value.toolCalls?.let {
            map["tool_calls"] =
                jsonEncoder.json.encodeToJsonElement(
                    kotlinx.serialization.builtins.ListSerializer(ToolCallDto.serializer()),
                    it,
                )
        }
        value.toolCallId?.let { map["tool_call_id"] = kotlinx.serialization.json.JsonPrimitive(it) }
        value.name?.let { map["name"] = kotlinx.serialization.json.JsonPrimitive(it) }

        jsonEncoder.encodeJsonElement(kotlinx.serialization.json.JsonObject(map))
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): ChatMessageDto {
        val jsonDecoder =
            decoder as? kotlinx.serialization.json.JsonDecoder
                ?: error("ChatMessageDtoSerializer only supports JSON")

        val obj = jsonDecoder.decodeJsonElement().let { it as kotlinx.serialization.json.JsonObject }
        val role = (obj["role"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "assistant"

        val contentElement = obj["content"]
        var contentText: String? = null
        var contentParts: List<ContentPartDto>? = null

        when (contentElement) {
            is kotlinx.serialization.json.JsonPrimitive -> contentText = contentElement.contentOrNull
            is kotlinx.serialization.json.JsonArray ->
                contentParts =
                    jsonDecoder.json.decodeFromJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(ContentPartDto.serializer()),
                        contentElement,
                    )
            else -> Unit
        }

        val toolCalls =
            (obj["tool_calls"] as? kotlinx.serialization.json.JsonArray)?.let {
                jsonDecoder.json.decodeFromJsonElement(kotlinx.serialization.builtins.ListSerializer(ToolCallDto.serializer()), it)
            }
        val toolCallId = (obj["tool_call_id"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
        val name = (obj["name"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull

        return ChatMessageDto(
            role = role,
            contentText = contentText,
            contentParts = contentParts,
            toolCalls = toolCalls,
            toolCallId = toolCallId,
            name = name,
        )
    }
}

@Serializable
data class ContentPartDto(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrlDto? = null,
)

@Serializable
data class ImageUrlDto(
    val url: String,
)

@Serializable
data class ToolDefinitionDto(
    val type: String = "function",
    val function: FunctionDefinitionDto,
)

@Serializable
data class FunctionDefinitionDto(
    val name: String,
    val description: String,
    val parameters: JsonElement,
)

@Serializable
data class ToolCallDto(
    val id: String,
    val type: String = "function",
    val function: FunctionCallDto,
)

@Serializable
data class FunctionCallDto(
    val name: String,
    val arguments: String,
)

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val choices: List<ChoiceDto> = emptyList(),
    val usage: UsageDto? = null,
    val error: ApiErrorDto? = null,
)

@Serializable
data class ChoiceDto(
    val index: Int = 0,
    val message: ChatMessageDto,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class UsageDto(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0,
)

@Serializable
data class ApiErrorDto(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null,
)

/**
 * v0.4.30 REAL STREAMING: OpenAI-compatible SSE delta-chunk shape,
 * received one per `data: {...}` line while `stream: true` is set on
 * the request. Previously ArtificerX never actually sent stream=true
 * anywhere and always waited for one full ChatCompletionResponse
 * before showing anything — the "streaming" chat bubble was fake, a
 * single AgentTextChunk containing the entire finished reply. These
 * DTOs are what AgentOrchestrator.streamCloudProvider parses per-line
 * to emit real incremental AgentTextChunk events as tokens actually
 * arrive from Groq/OpenRouter, and to accumulate tool-call deltas
 * (which providers also stream in fragments, indexed by call slot)
 * into complete tool calls once the stream ends.
 */
@Serializable
data class ChatCompletionStreamChunkDto(
    val choices: List<StreamChoiceDto> = emptyList(),
)

@Serializable
data class StreamChoiceDto(
    val index: Int = 0,
    val delta: StreamDeltaDto = StreamDeltaDto(),
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class StreamDeltaDto(
    val role: String? = null,
    val content: String? = null,
    @SerialName("tool_calls") val toolCalls: List<StreamToolCallDeltaDto>? = null,
)

@Serializable
data class StreamToolCallDeltaDto(
    val index: Int = 0,
    val id: String? = null,
    val type: String? = null,
    val function: StreamFunctionCallDeltaDto? = null,
)

@Serializable
data class StreamFunctionCallDeltaDto(
    val name: String? = null,
    val arguments: String? = null,
)
