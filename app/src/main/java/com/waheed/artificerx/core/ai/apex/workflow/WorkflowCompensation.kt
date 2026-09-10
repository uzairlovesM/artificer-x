package com.waheed.artificerx.core.ai.apex.workflow

data class CompensationAction(val stepId: String, val action: () -> Unit)
class CompensationStack {
    private val actions = ArrayDeque<CompensationAction>()
    fun push(action: CompensationAction) { actions.add(action) }
    fun rollback(): List<String> {
        val errors = mutableListOf<String>()
        while (actions.isNotEmpty()) {
            val item = actions.removeLast()
            try { item.action() } catch (t: Throwable) { errors += item.stepId + ":" + (t.message ?: "rollback_failed") }
        }
        return errors
    }
}
