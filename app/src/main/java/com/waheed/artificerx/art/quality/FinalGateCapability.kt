package com.waheed.artificerx.art.quality

/** Concrete capability contract for final gate.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class FinalGateCapability(val id:String="final_gate", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
