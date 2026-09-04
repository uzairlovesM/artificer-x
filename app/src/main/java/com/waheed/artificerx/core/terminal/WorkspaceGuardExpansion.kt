package com.waheed.artificerx.core.terminal

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for WorkspaceGuard. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class WorkspaceGuardExpansion : ExpansionCapability {
    override val id: String = "core.terminal.workspaceguard"
    override val area: String = "core.terminal"
    override val purpose: String = "WorkspaceGuard coordinates core.terminal responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("WorkspaceGuard.input", "WorkspaceGuard.state", "WorkspaceGuard.output", "WorkspaceGuard.failure", "WorkspaceGuard.telemetry")

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
