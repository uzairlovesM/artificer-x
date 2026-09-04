package com.waheed.artificerx.data.repository

/** Concrete capability contract for project repository.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class ProjectRepositoryCapability(val id:String="project_repository", val version:Int=1, val requires:Set<String>=emptySet(), val produces:Set<String>=emptySet(), val reversible:Boolean=true)
