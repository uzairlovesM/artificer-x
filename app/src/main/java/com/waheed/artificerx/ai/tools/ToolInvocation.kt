package com.waheed.artificerx.ai.tools

data class ToolInvocation(val toolId:String,val arguments:Map<String,Any?>,val reason:String,val expectedEvidence:List<String>,val attempt:Int=1)
data class ToolResult(val toolId:String,val success:Boolean,val output:Any?,val error:String?=null,val evidence:List<String> = emptyList())
