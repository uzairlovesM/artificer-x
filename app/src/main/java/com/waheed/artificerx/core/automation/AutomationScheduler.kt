package com.waheed.artificerx.core.automation

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

data class AutomationTask(val id: String, val name: String, val runAtMillis: Long, val priority: Int = 0)

@Singleton
class AutomationScheduler @Inject constructor() {
    private val tasks = ConcurrentHashMap<String, AutomationTask>()
    private val runs = AtomicLong()

    fun schedule(task: AutomationTask) { tasks[task.id] = task }
    fun cancel(id: String): Boolean = tasks.remove(id) != null

    fun due(nowMillis: Long): List<AutomationTask> =
        tasks.values.filter { it.runAtMillis <= nowMillis }.sortedWith(
            compareByDescending<AutomationTask> { it.priority }.thenBy { it.runAtMillis }
        )

    fun claimDue(nowMillis: Long): List<AutomationTask> {
        val due = due(nowMillis)
        due.forEach { if (tasks.remove(it.id) != null) runs.incrementAndGet() }
        return due
    }

    fun executionCount(): Long = runs.get()
}
