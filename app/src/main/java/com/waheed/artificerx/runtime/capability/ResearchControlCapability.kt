package com.waheed.artificerx.runtime.capability

/** Concrete capability contract for research control.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class ResearchControlCapability(val id: String = "research_control", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
