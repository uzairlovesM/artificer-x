package com.waheed.artificerx.domain.usecase.ai

/** Concrete capability contract for build context.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class BuildContextCapability(val id:String="build_context", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
