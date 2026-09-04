package com.waheed.artificerx.domain.usecase.art

/** Concrete capability contract for manage layers.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class ManageLayersCapability(val id: String = "manage_layers", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
