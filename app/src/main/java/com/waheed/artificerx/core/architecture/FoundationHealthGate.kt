package com.waheed.artificerx.core.architecture

import com.waheed.artificerx.core.project.ProjectSnapshot
import com.waheed.artificerx.core.project.ProjectValidator

data class FoundationCheck(
    val id: String,
    val healthy: Boolean,
    val details: String,
)

class FoundationHealthGate(
    private val validator: ProjectValidator = ProjectValidator(),
) {
    fun checkProject(snapshot: ProjectSnapshot): FoundationCheck {
        val issues = validator.validate(snapshot)
        val fatal = issues.filter { it.fatal }
        return FoundationCheck(
            id = "project-integrity",
            healthy = fatal.isEmpty(),
            details = if (fatal.isEmpty()) "project invariants satisfied" else fatal.joinToString("; ") { it.path + ": " + it.message },
        )
    }

    fun checkBoundary(owner: RuntimeBoundary, dependencies: Collection<RuntimeBoundary>): FoundationCheck {
        val violations = ArchitectureRules.validate(owner, dependencies)
        return FoundationCheck(
            id = "boundary-$owner",
            healthy = violations.isEmpty(),
            details = if (violations.isEmpty()) "dependency boundary satisfied" else violations.joinToString("; ") { "${it.owner} -> ${it.dependency}" },
        )
    }
}
