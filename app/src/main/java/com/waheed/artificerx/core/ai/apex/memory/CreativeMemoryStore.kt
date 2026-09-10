package com.waheed.artificerx.core.ai.apex.memory

import java.security.MessageDigest

enum class MemoryScope { GLOBAL, PROJECT, CHAT, CHARACTER, STYLE, BRUSH, PALETTE }

data class CreativeMemory(val id: String, val scope: MemoryScope, val text: String, val weight: Double, val createdAt: Long, var lastUsedAt: Long = createdAt, var uses: Long = 0)

class CreativeMemoryStore(private val capacity: Int = 5_000) {
    private val entries = LinkedHashMap<String, CreativeMemory>()
    @Synchronized fun remember(scope: MemoryScope, text: String, weight: Double = 0.5, now: Long = System.currentTimeMillis()): CreativeMemory {
        require(text.isNotBlank()); require(weight in 0.0..1.0)
        val id = digest(scope.name + "|" + text.trim())
        val current = entries[id]
        val value = if (current == null) CreativeMemory(id, scope, text.trim(), weight, now) else current.copy(weight = maxOf(current.weight, weight), lastUsedAt = now, uses = current.uses + 1)
        entries[id] = value
        trim()
        return value
    }
    @Synchronized fun recall(query: String, scope: MemoryScope? = null, limit: Int = 12): List<CreativeMemory> {
        val q = query.lowercase().split(Regex("\\W+")).filter(String::isNotBlank).toSet()
        return entries.values.asSequence().filter { scope == null || it.scope == scope }.map { memory ->
            val words = memory.text.lowercase().split(Regex("\\W+")).toSet()
            val overlap = if (q.isEmpty()) 0.0 else q.intersect(words).size.toDouble() / q.size
            Triple(memory, overlap + memory.weight * 0.25 + (memory.uses.coerceAtMost(20) / 100.0), overlap)
        }.filter { it.third > 0.0 || it.first.weight >= 0.8 }.sortedByDescending { it.second }.take(limit).map { it.first.also { it.lastUsedAt = System.currentTimeMillis(); it.uses++ } }.toList()
    }
    @Synchronized fun forget(id: String): Boolean = entries.remove(id) != null
    @Synchronized fun decay(now: Long = System.currentTimeMillis(), halfLifeDays: Double = 30.0) { entries.entries.forEach { (id, memory) -> val ageDays = (now - memory.lastUsedAt).coerceAtLeast(0L) / 86_400_000.0; val factor = Math.pow(0.5, ageDays / halfLifeDays); if (memory.weight * factor < 0.05) entries.remove(id) } }
    @Synchronized fun size(): Int = entries.size
    private fun trim() { while (entries.size > capacity) entries.remove(entries.values.minByOrNull { it.weight + it.uses / 100.0 }?.id) }
    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
