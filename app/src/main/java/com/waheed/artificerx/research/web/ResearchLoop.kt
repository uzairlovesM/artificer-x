package com.waheed.artificerx.research.web

data class ResearchSource(val url: String, val title: String, val relevance: Float, val evidence: String)
data class ResearchBundle(val query: String, val sources: List<ResearchSource>, val contradictions: List<String>)

class ResearchLoop {
    fun rank(query: String, sources: List<ResearchSource>): ResearchBundle {
        val ranked = sources.sortedByDescending { it.relevance }
        val contradictions = ranked.zipWithNext().filter { (a, b) -> a.evidence != b.evidence && a.relevance > .7f && b.relevance > .7f }.map { "Conflicting evidence: ${it.first.title} vs ${it.second.title}" }
        return ResearchBundle(query, ranked, contradictions)
    }
}
