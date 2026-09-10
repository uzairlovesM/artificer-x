package com.waheed.artificerx.automation

import com.waheed.artificerx.core.foundation.SubsystemHealth
import kotlin.math.min

class AutomationRuntime {
    data class Job(val id: String, val priority: Int, val dependencies: Set<String>, val action: suspend () -> String)
    data class Result(val id: String, val output: String?, val attempts: Int, val success: Boolean)

    private val completed = linkedSetOf<String>()
    private val failed = linkedSetOf<String>()
    private var executions = 0L

    suspend fun run(jobs: List<Job>, maxAttempts: Int = 3): List<Result> {
        val pending = jobs.associateBy { it.id }.toMutableMap()
        val results = mutableListOf<Result>()
        require(pending.keys.size == jobs.size) { "Duplicate automation job id" }
        while (pending.isNotEmpty()) {
            val ready = pending.values.filter { it.dependencies.all(completed::contains) }.sortedByDescending { it.priority }
            if (ready.isEmpty()) break
            for (job in ready) {
                pending.remove(job.id)
                var output: String? = null
                var success = false
                var attempts = 0
                while (attempts < maxAttempts.coerceAtLeast(1) && !success) {
                    attempts++
                    executions++
                    runCatching { job.action() }.onSuccess { output = it; success = true }
                }
                if (success) completed += job.id else failed += job.id
                results += Result(job.id, output, attempts, success)
            }
        }
        return results
    }

    fun retryDelay(attempt: Int, baseMs: Long = 250, capMs: Long = 30_000): Long {
        val safeAttempt = attempt.coerceIn(0, 16)
        return min(capMs, baseMs * (1L shl safeAttempt))
    }

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "automation",
        readiness = if (failed.isEmpty()) 1.0 else 0.85,
        capabilities = setOf("dependency-scheduling", "retry", "priority", "failure-isolation"),
        invariants = listOf("dependency-complete-before-run", "bounded-attempts"),
        counters = mapOf("executions" to executions, "completed" to completed.size.toLong(), "failed" to failed.size.toLong()),
    )
}
