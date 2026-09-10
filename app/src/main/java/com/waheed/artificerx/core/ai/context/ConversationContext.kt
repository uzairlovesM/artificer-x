package com.waheed.artificerx.core.ai.context

data class ContextMessage(
    val role: String,
    val content: String,
    val timestamp: Long,
    val tokenEstimate: Int
)

class ConversationContext(private val maxMessages: Int = 200) {
    private val messages = ArrayDeque<ContextMessage>()
    fun add(message: ContextMessage) {
        if (message.content.isBlank()) return
        messages.addLast(message)
        while (messages.size > maxMessages) messages.removeFirst()
    }
    fun all(): List<ContextMessage> = messages.toList()
    fun estimateTokens(): Int = messages.sumOf { it.tokenEstimate.coerceAtLeast(0) }
    fun clear() = messages.clear()
}
