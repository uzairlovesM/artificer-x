package com.waheed.artificerx.ui.screens.settings

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for SettingsCommands. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class SettingsCommandsExpansion : ExpansionCapability {
    override val id: String = "ui.screens.settings.settingscommands"
    override val area: String = "ui.screens.settings"
    override val purpose: String = "SettingsCommands coordinates ui.screens.settings responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("SettingsCommands.input", "SettingsCommands.state", "SettingsCommands.output", "SettingsCommands.failure", "SettingsCommands.telemetry")

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
