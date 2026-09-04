package com.waheed.artificerx.ai.drawing

/** Concrete capability contract for add material.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class AddMaterialCapability(
    val id: String = "add_material",
    val version: Int = 1,
    val requires: Set<String> = emptySet(),
    val produces: Set<String> = emptySet(),
    val reversible: Boolean = true,
)
