package com.waheed.artificerx.core.importexport

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ArtifactManifestEntry(val name: String, val mimeType: String, val sizeBytes: Long, val sha256: String? = null)
@Serializable
data class ArtifactManifest(val version: Int = 1, val createdAtEpochMillis: Long = System.currentTimeMillis(), val entries: List<ArtifactManifestEntry>)

object ArtifactManifestCodec {
    private val json = Json { prettyPrint = true; encodeDefaults = true }
    fun encode(manifest: ArtifactManifest): String = json.encodeToString<ArtifactManifest>(manifest)
    fun decode(raw: String): ArtifactManifest? = runCatching { json.decodeFromString<ArtifactManifest>(raw) }.getOrNull()
}
