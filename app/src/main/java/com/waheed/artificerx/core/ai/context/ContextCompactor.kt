package com.waheed.artificerx.core.ai.context

class ContextCompactor {
    fun compact(messages: List<ContextMessage>, targetTokens: Int): List<ContextMessage> {
        require(targetTokens > 0)
        if (messages.sumOf { it.tokenEstimate } <= targetTokens) return messages
        val output = ArrayDeque<ContextMessage>()
        var total = 0
        for (message in messages.asReversed()) {
            if (total + message.tokenEstimate > targetTokens) continue
            output.addFirst(message)
            total += message.tokenEstimate
        }
        return output.toList()
    }
}
