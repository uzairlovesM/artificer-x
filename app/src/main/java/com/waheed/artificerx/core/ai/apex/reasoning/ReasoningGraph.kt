package com.waheed.artificerx.core.ai.apex.reasoning

enum class ReasoningState { CREATED, OBSERVING, PLANNING, EXECUTING, VERIFYING, REPAIRING, COMPLETE, FAILED }
data class ReasoningNode(val id: String, val statement: String, val confidence: Double, val evidence: Set<String> = emptySet())
data class ReasoningDecision(val state: ReasoningState, val nodes: List<ReasoningNode>, val selected: String?, val confidence: Double)

class ReasoningGraph {
    private var state = ReasoningState.CREATED
    private val nodes = LinkedHashMap<String, ReasoningNode>()
    fun transition(next: ReasoningState) {
        require(isValid(state, next)) { "Invalid reasoning transition: $state -> $next" }
        state = next
    }
    fun add(node: ReasoningNode) { require(node.id.isNotBlank()); require(node.confidence in 0.0..1.0); nodes[node.id] = node }
    fun decide(): ReasoningDecision {
        val selected = nodes.values.maxByOrNull { it.confidence }
        return ReasoningDecision(state, nodes.values.toList(), selected?.id, selected?.confidence ?: 0.0)
    }
    private fun isValid(from: ReasoningState, to: ReasoningState): Boolean = when (from) {
        ReasoningState.CREATED -> to == ReasoningState.OBSERVING
        ReasoningState.OBSERVING -> to == ReasoningState.PLANNING || to == ReasoningState.FAILED
        ReasoningState.PLANNING -> to == ReasoningState.EXECUTING || to == ReasoningState.FAILED
        ReasoningState.EXECUTING -> to == ReasoningState.VERIFYING || to == ReasoningState.FAILED
        ReasoningState.VERIFYING -> to == ReasoningState.COMPLETE || to == ReasoningState.REPAIRING || to == ReasoningState.FAILED
        ReasoningState.REPAIRING -> to == ReasoningState.EXECUTING || to == ReasoningState.FAILED
        ReasoningState.COMPLETE, ReasoningState.FAILED -> false
    }
}
