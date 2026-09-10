package com.waheed.artificerx.core.project

data class ValidationIssue(val path: String, val message: String, val fatal: Boolean)

class ProjectValidator {
    fun validate(snapshot: ProjectSnapshot): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        if (snapshot.layers.isEmpty()) issues += ValidationIssue("layers", "Project has no layers", false)
        val ids = snapshot.layers.map { it.id.value }
        if (ids.size != ids.toSet().size) issues += ValidationIssue("layers.id", "Duplicate layer id", true)
        if (snapshot.activeLayer != null && snapshot.activeLayer.value !in ids)
            issues += ValidationIssue("activeLayer", "Active layer does not exist", true)
        val orders = snapshot.layers.map { it.order }
        if (orders.size != orders.toSet().size) issues += ValidationIssue("layers.order", "Duplicate layer order", false)
        if (snapshot.metadata.revision < 0) issues += ValidationIssue("metadata.revision", "Negative revision", true)
        return issues
    }

    fun isSafe(snapshot: ProjectSnapshot): Boolean = validate(snapshot).none { it.fatal }
}
