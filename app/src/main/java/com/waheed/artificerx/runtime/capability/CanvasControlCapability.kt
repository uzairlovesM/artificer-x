package com.waheed.artificerx.runtime.capability

/** Concrete capability contract for canvas control.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class CanvasControlCapability(val id:String="canvas_control", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
