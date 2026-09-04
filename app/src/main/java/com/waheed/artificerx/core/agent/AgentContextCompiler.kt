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
        val ordered = history.filter { it.contentText().isNotBlank() }
        val selected = ArrayDeque<ChatMessageDto>()
        var used = system.contentText().length + user.contentText().length
        selected.addLast(user)
        for (message in ordered.asReversed()) {
            val cost = message.contentText().length.coerceAtLeast(1)
            if (used + cost > maxCharacters) break
            selected.addFirst(message)
            used += cost
        }
        return Result(listOf(system) + selected.toList(), ordered.size - (selected.size - 1).coerceAtLeast(0))
    }

    private fun ChatMessageDto.contentText(): String = contentText ?: ""
}
