package com.waheed.artificerx.core.ai.apex.resource

data class ExecutionBudget(val cpuUnits: Int = 100, val memoryBytes: Long = 512L * 1024 * 1024, val networkUnits: Int = 10, val toolCalls: Int = 64) {
    init { require(cpuUnits >= 0 && memoryBytes >= 0 && networkUnits >= 0 && toolCalls >= 0) }
    fun consume(cpu: Int = 0, memory: Long = 0, network: Int = 0, tools: Int = 0): ExecutionBudget? {
        if (cpu > cpuUnits || memory > memoryBytes || network > networkUnits || tools > toolCalls) return null
        return copy(cpuUnits = cpuUnits - cpu, memoryBytes = memoryBytes - memory, networkUnits = networkUnits - network, toolCalls = toolCalls - tools)
    }
}
