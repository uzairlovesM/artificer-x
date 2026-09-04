package com.waheed.artificerx.data.remote.client

/** Concrete capability contract for artifact upload.
 * The implementation is selected by the runtime broker; the contract is deliberately
 * independent of any single provider so the agent can fall back to local or remote engines. */
data class ArtifactUploadCapability(val id: String = "artifact_upload", val version: Int = 1, val requires: Set<String> = emptySet(), val produces: Set<String> = emptySet(), val reversible: Boolean = true)
