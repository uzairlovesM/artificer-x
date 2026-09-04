package com.waheed.artificerx.ui.screens.chat

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for ChatDiagnostics. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class ChatDiagnosticsExpansion : ExpansionCapability {
    override val id: String = "ui.screens.chat.chatdiagnostics"
    override val area: String = "ui.screens.chat"
    override val purpose: String = "ChatDiagnostics coordinates ui.screens.chat responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("ChatDiagnostics.input", "ChatDiagnostics.state", "ChatDiagnostics.output", "ChatDiagnostics.failure", "ChatDiagnostics.telemetry")

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
