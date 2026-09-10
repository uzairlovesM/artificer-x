package com.waheed.artificerx.core.ai.apex.hybrid

data class RouteExplanation(val selected: String?, val alternatives: List<String>, val reasons: List<String>, val constraints: List<String>)
class RouteExplainer {
    fun explain(selected: String?, alternatives: List<String>, reasons: List<String>, constraints: List<String>): RouteExplanation = RouteExplanation(selected, alternatives, reasons, constraints)
}
