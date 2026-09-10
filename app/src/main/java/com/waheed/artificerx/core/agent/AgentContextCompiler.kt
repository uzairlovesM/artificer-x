package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.network.ChatMessageDto

/** Keeps long-running conversations useful by trimming only old context first.
 * System + newest user/assistant/tool context always win over stale history. */
object AgentContextCompiler {
    data class Result(val messages: List<ChatMessageDto>, val droppedCount: Int)

    fun compile(
        system: ChatMessageDto,
        history: List<ChatMessageDto>,
        user: ChatMessageDto,
        maxCharacters: Int = Int.MAX_VALUE,
    ): Result {
        val ordered = history.filter { it.contentText().isNotBlank() || it.contentParts != null || it.toolCalls != null }
        val selected = ArrayDeque<ChatMessageDto>()
        var used = messageCost(system) + messageCost(user)
        selected.addLast(user)
        for (message in ordered.asReversed()) {
            val cost = messageCost(message).coerceAtLeast(1)
            if (used + cost > maxCharacters) break
            selected.addFirst(message)
            used += cost
        }
        return Result(listOf(system) + selected.toList(), ordered.size - (selected.size - 1).coerceAtLeast(0))
    }

    fun trimForProvider(messages: List<ChatMessageDto>, maxCharacters: Int): List<ChatMessageDto> {
        if (messages.isEmpty() || maxCharacters <= 0) return messages.takeLast(1)
        val system = messages.firstOrNull { it.role == "system" } ?: messages.first()
        val tail = messages.asReversed().filterNot { it === system }
        val selected = ArrayDeque<ChatMessageDto>()
        var used = messageCost(system)
        for (message in tail) {
            val cost = messageCost(message)
            if (used + cost > maxCharacters) break
            selected.addFirst(message)
            used += cost
        }
        return listOf(system) + selected.toList()
    }

    private fun messageCost(message: ChatMessageDto): Int {
        val text = message.contentText?.length ?: 0
        val parts = message.contentParts?.sumOf { (it.text?.length ?: 0) + (it.imageUrl?.url?.length ?: 0) + 32 } ?: 0
        val tools = message.toolCalls?.sumOf { (it.function.name.length + it.function.arguments.length + 64) } ?: 0
        return text + parts + tools + 32
    }

    private fun ChatMessageDto.contentText(): String = contentText ?: ""
}
