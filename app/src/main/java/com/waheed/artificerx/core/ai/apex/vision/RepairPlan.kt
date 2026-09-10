package com.waheed.artificerx.core.ai.apex.vision

data class RepairAction(val id: String, val issueCode: String, val priority: Int, val description: String)
data class RepairPlan(val actions: List<RepairAction>)
class VisualRepairPlanner {
    fun build(report: CriticReport): RepairPlan = RepairPlan(report.issues.sortedByDescending { it.severity }.mapIndexed { index, issue -> RepairAction("repair-$index", issue.code, (issue.severity * 100).toInt(), "Correct ${issue.message}") })
}
