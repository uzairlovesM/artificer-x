package com.waheed.artificerx.art.layer

/** Concrete capability contract for blend mode.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class BlendModeCapability(val id:String="blend_mode", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
