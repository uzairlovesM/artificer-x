package com.waheed.artificerx.core.security
class SecretScrubber {
    private val patterns=listOf(Regex("(?i)(authorization\\s*:\\s*bearer\\s+)[^\\s]+"),Regex("(?i)(api[_-]?key\\s*[:=]\\s*)[^\\s,]+"),Regex("\\bsk-[A-Za-z0-9_-]+"))
    fun scrub(text:String):String {
        var result=text
        patterns.forEach{p->result=p.replace(result){m->if(m.groupValues.size>1)m.groupValues[1]+"[REDACTED]" else "[REDACTED]"}}
        return result
    }
}
