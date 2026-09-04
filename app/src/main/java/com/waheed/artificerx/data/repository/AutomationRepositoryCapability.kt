package com.waheed.artificerx.data.repository

/** Concrete capability contract for automation repository.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class AutomationRepositoryCapability(val id:String="automation_repository", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
