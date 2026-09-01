package com.waheed.artificerx.core.network

import com.waheed.artificerx.core.agent.LocalGenerationResult
import com.waheed.artificerx.core.agent.LocalInferenceEngine
import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.domain.model.LocalModelInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID

/**
 * Local-inference counterpart to AgentOrchestrator's OkHttp-based
 * callProvider() (Section: Local Model provider). Most open GGUF
 * models — unlike Groq/OpenRouter's hosted models — have no native
 * OpenAI-style `tool_calls` field in their output; they only produce
 * plain text. So this adapter:
 *
 *  1. Flattens the ChatMessageDto list + ToolRegistry's tool schemas
 *     into a single prompt string, using each model's expected chat
 *     template markers where we can infer them, falling back to a
 *     clearly-delimited generic instruct format otherwise.
 *  2. Instructs the model, in-prompt, to emit tool calls as a fenced
 *     `<tool_call>{"name": ..., "arguments": {...}}</tool_call>` block
 *     — a convention several open-weights instruct models (Hermes,
 *     Qwen, and others) are actually fine-tuned to already produce,
 *     and which every other model can at least follow when told to.
 *  3. Parses that block back out of the raw generated text into the
 *     exact same ToolCallDto / ChatCompletionResponse shape the HTTP
 *     path produces, so AgentOrchestrator's loop (tool execution,
 *     vision feedback, Critic pass) needs zero awareness of whether
 *     the provider was local or remote.
 *
 * Vision input: when the active LocalModelInfo has an mmproj file,
 * the last user ContentPartDto with type "image_url" (a data: URI, as
 * built by AgentOrchestrator.visionFeedbackMessage) is passed straight
 * through as LlamaHelper's imagePath — mirroring exactly how the
 * remote vision-feedback loop already works, so local models get the
 * same self-correction capability as hosted ones.
 */
class LocalLlamaAdapter(
    private val engine: LocalInferenceEngine,
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    suspend fun ensureModelLoaded(model: LocalModelInfo): Boolean {
        if (engine.loadedModelId.value == model.id) return true
        return engine.loadModel(model)
    }

    suspend fun generate(
        model: LocalModelInfo,
        messages: List<ChatMessageDto>,
        temperature: Double,
    ): ChatCompletionResponse? {
        val loaded = ensureModelLoaded(model)
        if (!loaded) return null

        val prompt = buildPrompt(messages, model.isVisionCapable)
        val imageDataUri = if (model.isVisionCapable) extractLatestImageDataUri(messages) else null

        val result = engine.generateToCompletion(prompt, imageDataUri)
        return when (result) {
            is LocalGenerationResult.Success -> resultToResponse(result.fullText)
            is LocalGenerationResult.Failure, is LocalGenerationResult.Aborted -> null
        }
    }

    /** Builds a single instruct-style prompt from the full message
     *  history. Deliberately model-agnostic rather than trying to
     *  detect and apply each family's exact chat template (Llama-3,
     *  Gemma, Qwen, ChatML all differ) — llama.cpp itself applies the
     *  GGUF's embedded chat template internally when supported; this
     *  string is the content passed through predict() and works
     *  acceptably across families since every modern instruct model
     *  is trained to follow clearly delimited System/Tools/
     *  Conversation sections even without its exact template
     *  markers. */
    private fun buildPrompt(
        messages: List<ChatMessageDto>,
        includeToolSchemas: Boolean,
    ): String {
        val builder = StringBuilder()

        val systemMessage = messages.firstOrNull { it.role == "system" }
        if (systemMessage?.contentText != null) {
            builder.append("### SYSTEM\n").append(systemMessage.contentText).append("\n\n")
        }

        if (includeToolSchemas) {
            builder.append(TOOL_CALLING_INSTRUCTIONS).append("\n\n")
            builder.append("### AVAILABLE TOOLS (JSON Schema)\n")
            ToolRegistry.ALL_TOOLS.forEach { tool ->
                builder
                    .append("- ")
                    .append(tool.function.name)
                    .append(": ")
                    .append(tool.function.description)
                    .append("\n")
            }
            builder.append("\n")
        }

        builder.append("### CONVERSATION\n")
        messages.filter { it.role != "system" }.forEach { message ->
            appendMessage(builder, message)
        }

        builder.append("### ASSISTANT\n")
        return builder.toString()
    }

    private fun appendMessage(
        builder: StringBuilder,
        message: ChatMessageDto,
    ) {
        when (message.role) {
            "user" -> {
                val text =
                    message.contentText
                        ?: message.contentParts?.firstOrNull { it.type == "text" }?.text
                        ?: ""
                val hasImage = message.contentParts?.any { it.type == "image_url" } == true
                builder.append("USER: ").append(text)
                if (hasImage) builder.append(" [image attached]")
                builder.append("\n")
            }
            "assistant" -> {
                val text = message.contentText ?: ""
                if (text.isNotBlank()) builder.append("ASSISTANT: ").append(text).append("\n")
                message.toolCalls?.forEach { call ->
                    builder
                        .append("ASSISTANT: <tool_call>")
                        .append("""{"name": "${call.function.name}", "arguments": ${call.function.arguments}}""")
                        .append("</tool_call>\n")
                }
            }
            "tool" -> {
                builder
                    .append("TOOL RESULT (")
                    .append(message.name ?: "unknown")
                    .append("): ")
                    .append(message.contentText ?: "")
                    .append("\n")
            }
        }
    }

    private fun extractLatestImageDataUri(messages: List<ChatMessageDto>): String? =
        messages
            .asReversed()
            .firstOrNull { it.contentParts?.any { part -> part.type == "image_url" } == true }
            ?.contentParts
            ?.firstOrNull { it.type == "image_url" }
            ?.imageUrl
            ?.url

    /** Parses the model's raw text output, extracting `<tool_call>`
     *  blocks if present and shaping everything into the same
     *  ChatCompletionResponse the remote HTTP path produces —
     *  AgentOrchestrator's loop consumes this identically regardless
     *  of origin. Multiple tool_call blocks in one response are all
     *  collected (some models emit several calls per turn). */
    private fun resultToResponse(rawText: String): ChatCompletionResponse {
        val toolCallMatches = TOOL_CALL_PATTERN.findAll(rawText).toList()

        if (toolCallMatches.isEmpty()) {
            val cleaned = rawText.trim()
            return ChatCompletionResponse(
                id = UUID.randomUUID().toString(),
                choices =
                    listOf(
                        ChoiceDto(
                            message = ChatMessageDto(role = "assistant", contentText = cleaned),
                            finishReason = "stop",
                        ),
                    ),
            )
        }

        val toolCalls =
            toolCallMatches.mapNotNull { match ->
                parseToolCallJson(match.groupValues[1])
            }

        val leadingText = rawText.substring(0, toolCallMatches.first().range.first).trim()

        return ChatCompletionResponse(
            id = UUID.randomUUID().toString(),
            choices =
                listOf(
                    ChoiceDto(
                        message =
                            ChatMessageDto(
                                role = "assistant",
                                contentText = leadingText.ifBlank { null },
                                toolCalls = toolCalls.ifEmpty { null },
                            ),
                        finishReason = if (toolCalls.isNotEmpty()) "tool_calls" else "stop",
                    ),
                ),
        )
    }

    private fun parseToolCallJson(rawJson: String): ToolCallDto? =
        runCatching {
            val obj = json.parseToJsonElement(rawJson.trim()) as JsonObject
            val name = (obj["name"] as? JsonPrimitive)?.content ?: return null
            val argumentsElement: JsonElement = obj["arguments"] ?: JsonObject(emptyMap())
            ToolCallDto(
                id = "local_${UUID.randomUUID()}",
                function =
                    FunctionCallDto(
                        name = name,
                        arguments = json.encodeToString(JsonElement.serializer(), argumentsElement),
                    ),
            )
        }.getOrNull()

    private companion object {
        val TOOL_CALL_PATTERN = Regex("<tool_call>(.*?)</tool_call>", RegexOption.DOT_MATCHES_ALL)

        val TOOL_CALLING_INSTRUCTIONS =
            """
            You can call tools by responding with a fenced block in exactly this format:
            <tool_call>{"name": "tool_name", "arguments": {"param": "value"}}</tool_call>
            You may emit multiple <tool_call> blocks in one response if you need to call
            several tools before your next turn. When you are done and need no more tools,
            respond with plain text, or call finish_turn.
            """.trimIndent()
    }
}
