package com.waheed.artificerx.core.ai.apex.model

enum class ModelMode { OFFLINE, FAST, QUALITY, VISION, REASONING, HYBRID }

data class ModelEndpoint(val id: String, val local: Boolean, val supportsVision: Boolean, val supportsTools: Boolean, val maxContextTokens: Int, val health: Double = 1.0)

data class ModelSelection(val endpoint: ModelEndpoint, val score: Double, val reasons: List<String>)

class ModelRouter(private val endpoints: MutableList<ModelEndpoint> = mutableListOf()) {
    fun register(endpoint: ModelEndpoint) { require(endpoint.id.isNotBlank()); endpoints.removeAll { it.id == endpoint.id }; endpoints += endpoint }
    fun providerCount(): Int = endpoints.size
    fun select(mode: ModelMode, needsVision: Boolean, needsTools: Boolean, minContext: Int = 4_096): ModelSelection? {
        val candidates = endpoints.filter { it.maxContextTokens >= minContext && (!needsVision || it.supportsVision) && (!needsTools || it.supportsTools) }
        return candidates.map { endpoint ->
            var score = endpoint.health * 100.0
            val reasons = mutableListOf<String>()
            if (mode == ModelMode.OFFLINE && endpoint.local) { score += 30; reasons += "local" }
            if (mode != ModelMode.OFFLINE && !endpoint.local) { score += 10; reasons += "remote" }
            if (needsVision && endpoint.supportsVision) { score += 20; reasons += "vision" }
            if (needsTools && endpoint.supportsTools) { score += 20; reasons += "tools" }
            if (endpoint.maxContextTokens >= minContext * 2) { score += 5; reasons += "context" }
            ModelSelection(endpoint, score, reasons)
        }.maxByOrNull { it.score }
    }
}
