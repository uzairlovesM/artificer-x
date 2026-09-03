package com.waheed.artificerx.core.workflow

import kotlinx.coroutines.delay

@kotlinx.serialization.Serializable
data class WorkflowStep(val id: String, val name: String, val action: String, val retryLimit: Int = 1)
@kotlinx.serialization.Serializable
data class WorkflowDefinition(val id: String, val name: String, val steps: List<WorkflowStep>)
data class WorkflowStepResult(val step: WorkflowStep, val success: Boolean, val output: String, val attempts: Int)
data class WorkflowRunResult(val workflow: WorkflowDefinition, val steps: List<WorkflowStepResult>, val success: Boolean)

fun interface WorkflowActionRunner { suspend fun run(action: String): Result<String> }

class WorkflowEngine {
    suspend fun run(definition: WorkflowDefinition, runner: WorkflowActionRunner): WorkflowRunResult {
        val results = mutableListOf<WorkflowStepResult>()
        for (step in definition.steps) {
            var attempt = 0
            var last = ""
            var ok = false
            while (attempt <= step.retryLimit && !ok) {
                attempt++
                val result = runCatching { runner.run(step.action) }.getOrElse { Result.failure(it) }
                ok = result.isSuccess
                last = result.getOrElse { it.message ?: "Action failed" }
                if (!ok && attempt <= step.retryLimit) delay((attempt * 250L).coerceAtMost(1_500L))
            }
            results += WorkflowStepResult(step, ok, last, attempt)
            if (!ok) return WorkflowRunResult(definition, results, false)
        }
        return WorkflowRunResult(definition, results, true)
    }
}
