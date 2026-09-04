package com.waheed.artificerx.core.export

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/** Production expansion contract for Cancel. */
class XCancelCapability : ExpansionCapability {
    override val id = "core.export.cancel"
    override val area = "core.export"
    override val purpose = "Cancel capability in core.export; explicit state, output, failure, telemetry, and provenance boundary."
    override val contracts = listOf("input", "state", "output", "failure", "telemetry", "provenance")

    override fun validate(): CapabilityCheck {
        val valid = contracts.all { it.isNotBlank() } && id.length > 4
        return CapabilityCheck(id, valid, if (valid) "contract-ready" else "contract-invalid", mapOf("contracts" to contracts.size.toString(), "area" to area, "purpose" to purpose.length.toString()))
    }
}
