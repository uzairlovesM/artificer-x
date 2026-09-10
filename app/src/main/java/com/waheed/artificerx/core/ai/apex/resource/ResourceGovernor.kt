package com.waheed.artificerx.core.ai.apex.resource

class ResourceGovernor(initial: ExecutionBudget = ExecutionBudget()) {
    private var budget = initial
    @Synchronized fun remaining(): ExecutionBudget = budget
    @Synchronized fun reserve(cpu: Int = 0, memory: Long = 0, network: Int = 0, tools: Int = 0): Boolean { val next = budget.consume(cpu, memory, network, tools) ?: return false; budget = next; return true }
    @Synchronized fun refund(cpu: Int = 0, memory: Long = 0, network: Int = 0, tools: Int = 0) { budget = budget.copy(cpuUnits = budget.cpuUnits + cpu, memoryBytes = budget.memoryBytes + memory, networkUnits = budget.networkUnits + network, toolCalls = budget.toolCalls + tools) }
}
