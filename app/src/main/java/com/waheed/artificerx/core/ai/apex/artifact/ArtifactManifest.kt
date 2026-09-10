package com.waheed.artificerx.core.ai.apex.artifact

import java.security.MessageDigest

data class ArtifactManifest(val id: String, val type: String, val sizeBytes: Long, val sha256: String, val metadata: Map<String, String>)
object ArtifactManifestBuilder {
    fun fromBytes(id: String, type: String, bytes: ByteArray, metadata: Map<String, String> = emptyMap()): ArtifactManifest = ArtifactManifest(id, type, bytes.size.toLong(), MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }, metadata.toSortedMap())
}
