package com.waheed.artificerx.ui.screens.chat

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for ChatReasoningTrace. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class ChatReasoningTraceExpansion : ExpansionCapability {
    override val id: String = "ui.screens.chat.chatreasoningtrace"
    override val area: String = "ui.screens.chat"
    override val purpose: String = "ChatReasoningTrace coordinates ui.screens.chat responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("ChatReasoningTrace.input", "ChatReasoningTrace.state", "ChatReasoningTrace.output", "ChatReasoningTrace.failure", "ChatReasoningTrace.telemetry")

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
