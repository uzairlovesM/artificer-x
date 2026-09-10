package com.waheed.artificerx.core.agent
class PlanGuard {
    fun validate(toolNames:List<String>,contracts:Map<String,ToolContract>,maxSteps:Int=64):Result<Unit> = runCatching {
        require(toolNames.size<=maxSteps){"Plan exceeds step limit"}
        require(toolNames.distinct().size==toolNames.size){"Duplicate tool invocation requires explicit loop handling"}
        toolNames.forEach{require(contracts.containsKey(it)){"Unknown tool: $it"}}
    }
}
