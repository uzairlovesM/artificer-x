package com.waheed.artificerx.automation.engine

/** Concrete capability contract for emit artifact.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class EmitArtifactCapability(val id:String="emit_artifact", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
