package com.waheed.artificerx.core.agent

data class RepairPlan(val shouldRetry: Boolean, val retryDelayMs: Long, val guidance: String)

object AgentRepairPlanner {
    fun classify(error: String): RepairPlan {
        val e = error.lowercase()
        return when {
            "timeout" in e || "timed out" in e -> RepairPlan(true, 700, "Retry once with a smaller payload or narrower tool scope.")
            "not found" in e || "no layer" in e -> RepairPlan(true, 150, "Inspect current project state and refresh IDs before retrying.")
            "permission" in e -> RepairPlan(false, 0, "Ask the user to grant the required runtime or SAF permission.")
            "network" in e || "http" in e -> RepairPlan(true, 1000, "Try the next configured provider or retry with backoff.")
            "unsupported" in e -> RepairPlan(false, 0, "Choose a concrete supported tool instead of inventing a capability.")
            else -> RepairPlan(true, 350, "Retry with corrected arguments after inspecting state.")
        }
    }
}
