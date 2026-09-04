package com.waheed.artificerx.ui.screens.settings

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/**
 * Concrete expansion capability for SettingsImports. It is deliberately small in mutable state and
 * large in contract surface: callers can discover what the capability expects before invoking it.
 */
class SettingsImportsExpansion : ExpansionCapability {
    override val id: String = "ui.screens.settings.settingsimports"
    override val area: String = "ui.screens.settings"
    override val purpose: String = "SettingsImports coordinates ui.screens.settings responsibilities through explicit contracts, observable state, validation signals, and deterministic hand-off boundaries."
    override val contracts: List<String> = listOf("SettingsImports.input", "SettingsImports.state", "SettingsImports.output", "SettingsImports.failure", "SettingsImports.telemetry")

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
