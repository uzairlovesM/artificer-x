package com.waheed.artificerx.core.artifact

data class ArtifactRef(
    val id: String,
    val name: String,
    val mimeType: String,
    val path: String,
    val sizeBytes: Long,
)

data class ArtifactInput(val name: String, val bytes: ByteArray, val mimeType: String = "application/octet-stream")
