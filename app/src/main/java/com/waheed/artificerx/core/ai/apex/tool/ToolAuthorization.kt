package com.waheed.artificerx.core.ai.apex.tool

enum class ToolPermission { READ, WRITE, NETWORK, PROCESS, DANGEROUS }
data class ToolAuthorizationRequest(val toolId: String, val required: Set<ToolPermission>, val granted: Set<ToolPermission>, val confirmed: Boolean)
data class ToolAuthorizationDecision(val allowed: Boolean, val missing: Set<ToolPermission>, val reason: String)
class ToolAuthorizationEngine {
    fun evaluate(request: ToolAuthorizationRequest): ToolAuthorizationDecision {
        val missing = request.required - request.granted
        if (missing.isNotEmpty()) return ToolAuthorizationDecision(false, missing, "missing_permission")
        if (ToolPermission.DANGEROUS in request.required && !request.confirmed) return ToolAuthorizationDecision(false, setOf(ToolPermission.DANGEROUS), "confirmation_required")
        return ToolAuthorizationDecision(true, emptySet(), "authorized")
    }
}
