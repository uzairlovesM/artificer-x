package com.waheed.artificerx.art.brush

/** Concrete capability contract for velocity curve.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class VelocityCurveCapability(val id: String = "velocity_curve", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
