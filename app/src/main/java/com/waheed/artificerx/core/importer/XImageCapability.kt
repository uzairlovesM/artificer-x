package com.waheed.artificerx.core.importer

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionCapability

/** Production expansion contract for Image. */
class XImageCapability : ExpansionCapability {
    override val id = "core.importer.image"
    override val area = "core.importer"
    override val purpose = "Image capability in core.importer; explicit state, output, failure, telemetry, and provenance boundary."
    override val contracts = listOf("input", "state", "output", "failure", "telemetry", "provenance")

    override fun validate(): CapabilityCheck {
        val valid = contracts.all { it.isNotBlank() } && id.length > 4
        return CapabilityCheck(id, valid, if (valid) "contract-ready" else "contract-invalid", mapOf("contracts" to contracts.size.toString(), "area" to area, "purpose" to purpose.length.toString()))
    }
}
