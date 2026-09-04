package com.waheed.artificerx.ai.drawing

/** Concrete capability contract for parse scene.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class ParseSceneCapability(val id:String="parse_scene", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
