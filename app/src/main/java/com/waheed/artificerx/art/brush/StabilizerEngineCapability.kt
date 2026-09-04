package com.waheed.artificerx.art.brush

/** Concrete capability contract for stabilizer engine.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class StabilizerEngineCapability(val id: String = "stabilizer_engine", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
