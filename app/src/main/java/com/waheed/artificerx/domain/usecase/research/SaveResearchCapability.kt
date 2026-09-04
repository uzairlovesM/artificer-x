package com.waheed.artificerx.domain.usecase.research

/** Concrete capability contract for save research.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class SaveResearchCapability(val id: String = "save_research", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
