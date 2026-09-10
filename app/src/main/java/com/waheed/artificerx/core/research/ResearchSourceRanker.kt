package com.waheed.artificerx.core.research
data class ResearchSource(val url:String,val authority:Float,val freshness:Float,val relevance:Float,val primary:Boolean)
class ResearchSourceRanker {
    fun rank(sources:List<ResearchSource>):List<ResearchSource> = sources.sortedByDescending{
        it.relevance.coerceIn(0f,1f)*.45f+it.authority.coerceIn(0f,1f)*.30f+it.freshness.coerceIn(0f,1f)*.15f+(if(it.primary).1f else 0f)*.10f
    }
}
