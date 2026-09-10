package com.waheed.artificerx.core.ai.apex.hybrid

data class ProviderHealth(var successes: Long = 0, var failures: Long = 0, var ewmaLatencyMs: Double = 0.0) {
    fun score(): Double { val total = successes + failures; val reliability = if (total == 0L) 0.5 else successes.toDouble() / total; val latency = 1.0 / (1.0 + ewmaLatencyMs / 1000.0); return (reliability * 0.75 + latency * 0.25).coerceIn(0.0, 1.0) }
}
class ProviderHealthBook {
    private val data = LinkedHashMap<String, ProviderHealth>()
    @Synchronized fun record(provider: String, success: Boolean, latencyMs: Long) { val h = data.getOrPut(provider) { ProviderHealth() }; if (success) h.successes++ else h.failures++; h.ewmaLatencyMs = if (h.ewmaLatencyMs == 0.0) latencyMs.toDouble() else h.ewmaLatencyMs * 0.8 + latencyMs * 0.2 }
    @Synchronized fun score(provider: String): Double = data[provider]?.score() ?: 0.5
}
