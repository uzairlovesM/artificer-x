package com.waheed.artificerx.core.automation

data class WorkflowNode(val id: String, val next: List<String> = emptyList())

class WorkflowGraph(nodes: List<WorkflowNode>) {
    private val byId = nodes.associateBy { it.id }
    init {
        require(byId.size == nodes.size)
        require(nodes.all { node -> node.next.all(byId::containsKey) })
        topologicalOrder()
    }

    fun topologicalOrder(): List<WorkflowNode> {
        val state = mutableMapOf<String, Int>()
        val output = mutableListOf<WorkflowNode>()
        fun visit(id: String) {
            when (state[id]) {
                1 -> error("Workflow cycle detected at $id")
                2 -> return
            }
            state[id] = 1
            byId.getValue(id).next.forEach(::visit)
            state[id] = 2
            output += byId.getValue(id)
        }
        byId.keys.forEach(::visit)
        return output.asReversed()
    }
}
