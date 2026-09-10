package com.waheed.artificerx.core.ai.apex.agent

data class ScheduledAgent(val id: String, val priority: Int, val dependencies: Set<String> = emptySet())
data class AgentSchedule(val parallelWaves: List<List<ScheduledAgent>>, val cycles: Set<String>)

class AgentScheduler {
    fun schedule(agents: List<ScheduledAgent>): AgentSchedule {
        val byId = agents.associateBy { it.id }
        val cycles = agents.filter { reachesCycle(it.id, byId, mutableSetOf()) }.map { it.id }.toSet()
        val pending = agents.filterNot { it.id in cycles }.toMutableSet()
        val waves = mutableListOf<List<ScheduledAgent>>()
        val completed = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            val wave = pending.filter { it.dependencies.all(completed::contains) }.sortedByDescending { it.priority }
            if (wave.isEmpty()) break
            waves += wave
            completed += wave.map { it.id }
            pending.removeAll(wave.toSet())
        }
        return AgentSchedule(waves, cycles + pending.map { it.id })
    }
    private fun reachesCycle(id: String, graph: Map<String, ScheduledAgent>, visiting: MutableSet<String>): Boolean {
        if (!visiting.add(id)) return true
        val result = graph[id]?.dependencies?.any { graph.containsKey(it) && reachesCycle(it, graph, visiting.toMutableSet()) } ?: false
        return result
    }
}
