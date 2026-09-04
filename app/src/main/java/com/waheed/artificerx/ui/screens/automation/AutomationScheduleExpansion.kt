package com.waheed.artificerx.ui.screens.automation

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for AutomationSchedule. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class AutomationScheduleExpansion : ExpansionCapability {
    override val id: String = "ui.screens.automation.automationschedule"
    override val area: String = "ui.screens.automation"
    override val purpose: String = "AutomationSchedule coordinates ui.screens.automation responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("AutomationSchedule.input", "AutomationSchedule.state", "AutomationSchedule.output", "AutomationSchedule.failure", "AutomationSchedule.telemetry")

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
