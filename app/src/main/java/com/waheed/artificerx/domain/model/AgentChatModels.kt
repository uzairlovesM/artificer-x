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
    // v0.4.30: set once this turn's canvas output has been auto-saved to
    // Pictures/ARTIFICER-X, so AgentChatScreen can render a "Saved as X /
    // View / Share" affordance directly on the bubble instead of the user
    // having to go find it in a gallery app on their own.
    val autoSavedFileName: String? = null,
    val autoSavedUri: android.net.Uri? = null,
)
