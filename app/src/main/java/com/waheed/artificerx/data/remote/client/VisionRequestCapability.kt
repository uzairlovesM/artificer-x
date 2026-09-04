package com.waheed.artificerx.data.remote.client

/** Concrete capability contract for vision request.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class VisionRequestCapability(val id: String = "vision_request", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
