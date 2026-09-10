package com.waheed.artificerx.core.ai.apex.deep.integration

import java.util.UUID
import kotlin.math.max
import kotlin.math.min

 data class RuntimeModel(
    val id: String,
    val family: String,
    val contextWindow: Int,
    val vision: Boolean,
    val tools: Boolean,
    val streaming: Boolean,
    val structuredOutput: Boolean,
    val local: Boolean,
    val health: Double = 1.0,
 )

data class RuntimeRequest(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val modelHint: String? = null,
    val requiredContext: Int = 4096,
    val needsVision: Boolean = false,
    val needsTools: Boolean = false,
    val needsJson: Boolean = false,
    val preferLocal: Boolean = false,
)

data class RuntimeDecision(
    val modelId: String?,
    val score: Double,
    val reasons: List<String>,
    val degraded: Boolean,
)

data class RuntimeEvent(val requestId: String, val type: String, val detail: String, val index: Long)

class UniversalAiRuntimeKernel {
    private val models = LinkedHashMap<String, RuntimeModel>()
    private val events = ArrayDeque<RuntimeEvent>()
    private val memory = LinkedHashMap<String, Double>()
    private var eventIndex = 0L

    fun registerModel(model: RuntimeModel): Boolean {
        if (model.id.isBlank() || model.contextWindow <= 0) return false
        models[model.id] = model.copy(health = model.health.coerceIn(0.0, 1.0))
        emit("MODEL_REGISTER", model.id)
        return true
    }

    fun unregisterModel(id: String): Boolean = models.remove(id) != null

    fun models(): List<RuntimeModel> = models.values.toList()

    fun route(request: RuntimeRequest): RuntimeDecision {
        val eligible = models.values.filter { model ->
            model.contextWindow >= request.requiredContext &&
                (!request.needsVision || model.vision) &&
                (!request.needsTools || model.tools) &&
                (!request.needsJson || model.structuredOutput)
        }
        if (eligible.isEmpty()) {
            val degraded = models.values.maxByOrNull { compatibilityScore(it, request) }
            emit("ROUTE_DEGRADED", degraded?.id ?: "none")
            return RuntimeDecision(degraded?.id, degraded?.let { compatibilityScore(it, request) } ?: 0.0, listOf("no-perfect-match"), true)
        }
        val winner = eligible.maxByOrNull { compatibilityScore(it, request) }
        val score = winner?.let { compatibilityScore(it, request) } ?: 0.0
        emit("ROUTE", winner?.id ?: "none")
        return RuntimeDecision(winner?.id, score, explain(winner, request), false)
    }

    fun observeModel(id: String, success: Boolean, latencyMs: Long, tokens: Int = 0) {
        val model = models[id] ?: return
        val quality = if (success) 1.0 else 0.0
        val latencyPenalty = 1.0 / (1.0 + latencyMs.coerceAtLeast(0) / 5000.0)
        val throughputBonus = min(tokens.toDouble() / max(latencyMs.toDouble(), 1.0) * 100.0, 1.0)
        val sample = 0.65 * quality + 0.25 * latencyPenalty + 0.10 * throughputBonus
        models[id] = model.copy(health = model.health * 0.82 + sample * 0.18)
        emit("MODEL_OBSERVE", "$id:${models[id]?.health}")
    }

    fun remember(key: String, score: Double) {
        if (key.isBlank()) return
        memory[key] = score.coerceIn(-1.0, 1.0)
        while (memory.size > 4096) memory.remove(memory.keys.first())
    }

    fun recall(keys: Collection<String>, limit: Int = 8): List<Pair<String, Double>> =
        keys.mapNotNull { key -> memory[key]?.let { key to it } }.sortedByDescending { it.second }.take(limit.coerceAtLeast(0))

    fun events(limit: Int = 256): List<RuntimeEvent> = events.takeLast(limit.coerceAtLeast(0))

    private fun compatibilityScore(model: RuntimeModel, request: RuntimeRequest): Double {
        var score = model.health
        score += if (request.modelHint != null && model.id.equals(request.modelHint, true)) 0.9 else 0.0
        score += if (request.preferLocal && model.local) 0.25 else if (!request.preferLocal && !model.local) 0.08 else 0.0
        score += if (request.needsVision && model.vision) 0.2 else 0.0
        score += if (request.needsTools && model.tools) 0.2 else 0.0
        score += if (request.needsJson && model.structuredOutput) 0.2 else 0.0
        score += min(model.contextWindow.toDouble() / max(request.requiredContext, 1), 4.0) * 0.08
        return score
    }

    private fun explain(model: RuntimeModel?, request: RuntimeRequest): List<String> = buildList {
        if (model == null) add("no-model") else {
            add("health=${model.health}")
            if (request.needsVision) add(if (model.vision) "vision" else "vision-missing")
            if (request.needsTools) add(if (model.tools) "tools" else "tools-missing")
            if (request.needsJson) add(if (model.structuredOutput) "structured-output" else "structured-output-missing")
            if (request.preferLocal) add(if (model.local) "local-preferred" else "remote-fallback")
        }
    }

    private fun emit(type: String, detail: String) {
        eventIndex += 1
        events.add(RuntimeEvent("runtime", type, detail, eventIndex))
        while (events.size > 4096) events.removeFirst()
    }
}
