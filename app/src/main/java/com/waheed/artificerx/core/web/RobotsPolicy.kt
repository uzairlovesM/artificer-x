package com.waheed.artificerx.core.web
class RobotsPolicy {
    fun allows(userAgent:String,path:String,disallowRules:Map<String,List<String>>):Boolean {
        val rules=disallowRules[userAgent]?:disallowRules["*"]?:emptyList()
        val normalized=if(path.startsWith("/"))path else "/$path"
        return rules.none{rule->rule.isNotBlank() && normalized.startsWith(rule)}
    }
}
