package com.waheed.artificerx.core.ai.apex

data class ContextItem(val id: String, val kind: String, val text: String, val relevance: Double, val priority: Int)

data class ContextPack(val items: List<ContextItem>, val estimatedTokens: Int) {
    fun asPrompt(): String = items.joinToString("\n") { "[${it.kind}:${it.id}] ${it.text}" }
}

class ContextEngine {
    fun compile(items: Collection<ContextItem>, maxTokens: Int): ContextPack {
        require(maxTokens > 0)
        var used = 0
        val selected = items.sortedWith(compareByDescending<ContextItem> { it.priority }.thenByDescending { it.relevance })
            .filter {
                val cost = estimateTokens(it.text)
                if (used + cost <= maxTokens) { used += cost; true } else false
            }
        return ContextPack(selected, used)
    }

    private fun estimateTokens(text: String): Int = ((text.trim().length + 3) / 4).coerceAtLeast(1)
}
