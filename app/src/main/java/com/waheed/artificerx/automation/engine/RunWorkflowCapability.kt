package com.waheed.artificerx.automation.engine

/** Concrete capability contract for run workflow.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class RunWorkflowCapability(val id: String = "run_workflow", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
