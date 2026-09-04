package com.waheed.artificerx.ai.vision

data class VisualReference(val url: String, val title: String, val description: String, val relevance: Float)
class VisualResearchLoop(private val search: suspend (String)->List<VisualReference>) {
    suspend fun collect(plan: ReferenceResearchPlan): List<VisualReference> {
        val queries = (plan.visualQueries+plan.searchQueries).distinct()
        return queries.flatMap { search(it) }.distinctBy { it.url }.sortedByDescending { it.relevance }.take(24)
    }
}
