package com.waheed.artificerx.domain.usecase.runtime

/** Concrete capability contract for validate tool.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class ValidateToolCapability(val id: String = "validate_tool", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
