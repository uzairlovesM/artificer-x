package com.waheed.artificerx.init

import com.waheed.artificerx.core.foundation.SubsystemHealth

class StartupReadiness {
    data class Check(val id: String, val ok: Boolean, val detail: String)
    fun summarize(checks: List<Check>): SubsystemHealth {
        val failures = checks.filterNot { it.ok }
        return SubsystemHealth(
            id = "init",
            readiness = if (checks.isEmpty()) 0.0 else (checks.size - failures.size).toDouble() / checks.size,
            capabilities = setOf("startup-checks", "failure-reporting"),
            invariants = listOf("startup-checks-complete-before-ready"),
            counters = mapOf("checks" to checks.size.toLong(), "failures" to failures.size.toLong()),
            warnings = failures.map { "${it.id}:${it.detail}" },
        )
    }
}
