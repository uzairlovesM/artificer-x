package com.waheed.artificerx.domain.model

enum class ChatMessageRole {
    USER,
    AGENT,
    SYSTEM_TOOL_LOG,
}

enum class ToolCallStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
}

data class ToolCallEntry(
    val id: String,
    val toolName: String,
    val argsPreview: String,
    val status: ToolCallStatus,
    val resultSummary: String? = null,
)

data class ChatMessage(
    val id: String,
    val role: ChatMessageRole,
    val text: String,
    val toolCalls: List<ToolCallEntry> = emptyList(),
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val attachedImageUri: String? = null,
)
