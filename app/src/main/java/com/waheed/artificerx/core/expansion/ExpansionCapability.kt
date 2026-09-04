package com.waheed.artificerx.core.expansion

/** Runtime-discoverable production capability contributed by the expansion layer. */
interface ExpansionCapability {
    val id: String
    val area: String
    val purpose: String
    val contracts: List<String>
    fun validate(): CapabilityCheck
}

data class CapabilityCheck(
    val id: String,
    val ready: Boolean,
    val reason: String,
    val signals: Map<String, String>,
)
