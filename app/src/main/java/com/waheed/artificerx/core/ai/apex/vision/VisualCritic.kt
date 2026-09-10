package com.waheed.artificerx.core.ai.apex.vision

data class CriticIssue(val code: String, val message: String, val severity: Double)
data class CriticReport(val score: Double, val issues: List<CriticIssue>, val pass: Boolean)

class VisualCritic(private val diff: VisualDiffEngine = VisualDiffEngine()) {
    fun ready(): Boolean = true
    fun inspect(before: IntArray, after: IntArray, expectedChangeRatio: ClosedFloatingPointRange<Double>? = null): CriticReport {
        val metrics = diff.compare(before, after)
        val issues = mutableListOf<CriticIssue>()
        if (metrics.changedRatio < 0.00001 && expectedChangeRatio?.start ?: 0.0 > 0.0) issues += CriticIssue("NO_VISIBLE_CHANGE", "Expected a visible canvas change but the result is effectively identical.", 0.8)
        if (expectedChangeRatio != null && metrics.changedRatio !in expectedChangeRatio) issues += CriticIssue("CHANGE_SCOPE", "Visible change ratio is outside the requested scope.", 0.6)
        val score = (metrics.similarity - issues.sumOf { it.severity } * 0.2).coerceIn(0.0, 1.0)
        return CriticReport(score, issues, issues.none { it.severity >= 0.7 })
    }
}
