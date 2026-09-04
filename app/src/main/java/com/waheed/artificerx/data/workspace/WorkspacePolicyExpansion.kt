package com.waheed.artificerx.data.workspace

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for WorkspacePolicy. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class WorkspacePolicyExpansion : ExpansionCapability {
    override val id: String = "data.workspace.workspacepolicy"
    override val area: String = "data.workspace"
    override val purpose: String = "WorkspacePolicy coordinates data.workspace responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("WorkspacePolicy.input", "WorkspacePolicy.state", "WorkspacePolicy.output", "WorkspacePolicy.failure", "WorkspacePolicy.telemetry")

    override fun validate(): CapabilityCheck {
        val signalCount = contracts.count { it.isNotBlank() }
        return CapabilityCheck(
            id = id,
            ready = signalCount == contracts.size && contracts.isNotEmpty(),
            reason = if (signalCount == contracts.size) "contracts-ready" else "contract-gap",
            signals = mapOf(
                "contractCount" to signalCount.toString(),
                "area" to area,
                "purposeLength" to purpose.length.toString(),
            ),
        )
    }
}
