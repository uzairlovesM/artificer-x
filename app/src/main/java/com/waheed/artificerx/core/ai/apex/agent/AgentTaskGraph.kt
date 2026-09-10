package com.waheed.artificerx.core.ai.apex.agent

data class AgentTask(val projectId: String, val canvasRevision: Long, val prompt: String)
data class TaskNode(val id: String, val description: String, val dependsOn: Set<String>, val cost: Double)

data class AgentTaskGraph(val task: AgentTask, val nodes: List<TaskNode>) {
    init {
        require(task.projectId.isNotBlank())
        require(task.prompt.isNotBlank())
        require(nodes.isNotEmpty())
        val ids = nodes.map { it.id }.toSet()
        require(ids.size == nodes.size)
        nodes.forEach { require(it.dependsOn.all(ids::contains)) }
    }
    fun ready(completed: Set<String>): List<TaskNode> = nodes.filter { it.id !in completed && it.dependsOn.all(completed::contains) }
    fun totalCost(): Double = nodes.sumOf { it.cost }
}
