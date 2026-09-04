package com.waheed.artificerx.art.quality

/** Concrete capability contract for perspective gate.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class PerspectiveGateCapability(val id:String="perspective_gate", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
