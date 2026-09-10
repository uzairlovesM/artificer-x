package com.waheed.artificerx.ai

import com.waheed.artificerx.core.foundation.SubsystemHealth
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.exp

/** Coordinates model-agnostic AI pipeline state without owning a provider SDK. */
class AiPipelineRuntime {
    enum class Stage { IDLE, CONTEXT, ROUTING, INFERENCE, TOOLS, VERIFY, COMMITTED, FAILED }
    data class Signal(val name: String, val value: Double, val confidence: Double)
    data class Decision(val route: String, val score: Double, val reasons: List<String>)

    private var stage = Stage.IDLE
    private var lastDecision: Decision? = null
    private val calls = AtomicLong()
    private val failures = AtomicLong()
    private val verified = AtomicLong()
    private val latencySamples = ArrayDeque<Long>()

    fun transition(next: Stage) {
        val valid = when (stage) {
            Stage.IDLE -> next == Stage.CONTEXT || next == Stage.FAILED
            Stage.CONTEXT -> next == Stage.ROUTING || next == Stage.FAILED
            Stage.ROUTING -> next == Stage.INFERENCE || next == Stage.TOOLS || next == Stage.FAILED
            Stage.INFERENCE, Stage.TOOLS -> next == Stage.VERIFY || next == Stage.FAILED
            Stage.VERIFY -> next == Stage.COMMITTED || next == Stage.TOOLS || next == Stage.FAILED
            Stage.COMMITTED, Stage.FAILED -> next == Stage.IDLE
        }
        require(valid) { "Invalid AI stage transition $stage -> $next" }
        stage = next
    }

    fun route(candidates: Map<String, List<Signal>>): Decision {
        require(candidates.isNotEmpty())
        val ranked = candidates.map { (id, signals) ->
            val score = signals.map { signal ->
                val certainty = signal.confidence.coerceIn(0.0, 1.0)
                val quality = 1.0 / (1.0 + exp(-signal.value))
                quality * certainty
            }.averageOrZero()
            id to score
        }.sortedByDescending { it.second }
        val winner = ranked.first()
        val decision = Decision(winner.first, winner.second, listOf("signals=${candidates[winner.first].orEmpty().size}", "candidates=${candidates.size}"))
        lastDecision = decision
        calls.incrementAndGet()
        return decision
    }

    fun recordInference(startedAtMs: Long, success: Boolean) {
        latencySamples.add((System.currentTimeMillis() - startedAtMs).coerceAtLeast(0L))
        while (latencySamples.size > 128) latencySamples.removeFirst()
        if (success) verified.incrementAndGet() else failures.incrementAndGet()
    }

    fun reset() { require(stage == Stage.COMMITTED || stage == Stage.FAILED || stage == Stage.IDLE); stage = Stage.IDLE }

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "ai",
        readiness = when (stage) { Stage.IDLE, Stage.COMMITTED -> 1.0; Stage.FAILED -> 0.35; else -> 0.85 },
        capabilities = setOf("context", "routing", "inference", "tools", "verification"),
        invariants = listOf("stage-machine", "provider-neutral", "verified-before-commit"),
        counters = mapOf("calls" to calls.get(), "failures" to failures.get(), "verified" to verified.get(), "latencySamples" to latencySamples.size.toLong()),
        warnings = buildList { if (lastDecision == null) add("No model route has been selected in this runtime instance") },
    )

    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average().coerceIn(0.0, 1.0)
}
