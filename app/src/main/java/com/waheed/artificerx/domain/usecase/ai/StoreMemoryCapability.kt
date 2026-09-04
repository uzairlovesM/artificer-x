package com.waheed.artificerx.domain.usecase.ai

/** Concrete capability contract for store memory.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class StoreMemoryCapability(val id:String="store_memory", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
