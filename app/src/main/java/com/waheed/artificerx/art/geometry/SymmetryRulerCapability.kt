package com.waheed.artificerx.art.geometry

/** Concrete capability contract for symmetry ruler.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class SymmetryRulerCapability(val id:String="symmetry_ruler", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
