package com.waheed.artificerx.core.ai.apex.workflow

data class WorkflowCheckpoint(val workflowId: String, val completed: Set<String>, val outputs: Map<String, String>, val timestamp: Long = System.currentTimeMillis())
class WorkflowCheckpointStore {
    private val checkpoints = LinkedHashMap<String, WorkflowCheckpoint>()
    @Synchronized fun save(checkpoint: WorkflowCheckpoint) { checkpoints[checkpoint.workflowId] = checkpoint }
    @Synchronized fun load(id: String): WorkflowCheckpoint? = checkpoints[id]
    @Synchronized fun clear(id: String) { checkpoints.remove(id) }
}
