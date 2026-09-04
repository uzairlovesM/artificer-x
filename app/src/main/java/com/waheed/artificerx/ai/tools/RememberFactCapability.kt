package com.waheed.artificerx.ai.tools

/** Concrete capability contract for remember fact.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class RememberFactCapability(val id:String="remember_fact", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
