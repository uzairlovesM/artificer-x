package com.waheed.artificerx.core.architecture

import com.waheed.artificerx.core.agent.ExecutionJournal
import com.waheed.artificerx.core.agent.ToolContract
import com.waheed.artificerx.core.agent.ToolPolicy
import com.waheed.artificerx.core.diagnostics.PerformanceTracker
import com.waheed.artificerx.core.events.EventHub
import com.waheed.artificerx.core.runtime.CircuitBreaker
import com.waheed.artificerx.core.runtime.ResourceBudget

data class RuntimeSnapshot(
    val toolsRegistered: Int,
    val eventsPublished: Long,
    val successfulOperations: Long,
    val failedOperations: Long
)

class ArtificerRuntime(
    private val eventHub: EventHub = EventHub(),
    private val journal: ExecutionJournal = ExecutionJournal(),
    private val performance: PerformanceTracker = PerformanceTracker()
) {
    private val tools = LinkedHashMap<String, ToolContract>()
    private val breakers = HashMap<String, CircuitBreaker>()
    private var successes = 0L
    private var failures = 0L

    fun registerTool(contract: ToolContract) {
        tools[contract.name] = contract
        breakers.putIfAbsent(contract.name, CircuitBreaker())
        eventHub.publish("tool.registered", mapOf("name" to contract.name))
    }

    fun canExecute(name: String, network: Boolean, fileWrite: Boolean): Boolean {
        val contract = tools[name] ?: return false
        return ToolPolicy().allowed(contract, network, fileWrite, com.waheed.artificerx.core.agent.ToolRisk.HIGH) &&
            breakers.getValue(name).allowRequest()
    }

    fun recordExecution(name: String, startedAt: Long, finishedAt: Long, success: Boolean, summary: String) {
        val elapsed = (finishedAt - startedAt).coerceAtLeast(0L)
        journal.record(name, startedAt, finishedAt, success, summary)
        performance.record("tool.$name", elapsed)
        if (success) {
            successes++
            breakers[name]?.recordSuccess()
        } else {
            failures++
            breakers[name]?.recordFailure()
        }
        eventHub.publish(
            "tool.completed",
            mapOf("name" to name, "success" to success.toString(), "elapsedMs" to elapsed.toString())
        )
    }

    fun budgetForCanvas(width: Int, height: Int): ResourceBudget {
        val pixels = width.toLong() * height.toLong()
        val memory = (pixels * 4L).coerceAtMost(2L * 1024 * 1024 * 1024)
        val cpu = ((pixels / 1_000_000L).toInt() + 1).coerceAtMost(100)
        return ResourceBudget(memoryBytes = memory, cpuUnits = cpu, ioUnits = 10, networkUnits = 5)
    }

    fun snapshot(): RuntimeSnapshot = RuntimeSnapshot(tools.size, eventHub.count("tool.completed") + eventHub.count("tool.registered"), successes, failures)
}
