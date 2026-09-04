package com.waheed.artificerx.domain.usecase.runtime

/** Concrete capability contract for inspect ndk.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class InspectNdkCapability(val id:String="inspect_ndk", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
