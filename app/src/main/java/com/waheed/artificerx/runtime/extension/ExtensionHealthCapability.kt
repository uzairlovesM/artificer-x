package com.waheed.artificerx.runtime.extension

/** Concrete capability contract for extension health.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class ExtensionHealthCapability(val id:String="extension_health", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
