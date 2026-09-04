package com.waheed.artificerx.automation.engine

data class AgentWorkflow(val id:String,val name:String,val triggers:List<String>,val steps:List<String>,val retryCount:Int=2,val enabled:Boolean=true)
class WorkflowValidator {
    fun errors(workflow:AgentWorkflow):List<String> = buildList { if(workflow.id.isBlank())add("id");if(workflow.steps.isEmpty())add("steps");if(workflow.retryCount<0) add("retryCount") }
}
