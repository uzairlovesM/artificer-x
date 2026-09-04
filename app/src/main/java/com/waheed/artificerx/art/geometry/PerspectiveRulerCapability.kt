package com.waheed.artificerx.art.geometry

/** Concrete capability contract for perspective ruler.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class PerspectiveRulerCapability(val id: String = "perspective_ruler", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
