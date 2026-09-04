package com.waheed.artificerx.ai.vision

/** Concrete capability contract for detect objects.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class DetectObjectsCapability(val id: String = "detect_objects", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
