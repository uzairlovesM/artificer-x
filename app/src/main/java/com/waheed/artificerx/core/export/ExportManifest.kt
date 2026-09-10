package com.waheed.artificerx.core.export

import com.waheed.artificerx.core.security.Checksum

data class ExportFile(
    val path: String,
    val sizeBytes: Long,
    val sha256: String
)

data class ExportManifest(
    val formatVersion: Int,
    val projectId: String,
    val createdAt: Long,
    val files: List<ExportFile>
) {
    fun canonicalText(): String = buildString {
        append("version=").append(formatVersion).append('\n')
        append("project=").append(projectId).append('\n')
        append("created=").append(createdAt).append('\n')
        files.sortedBy { it.path }.forEach {
            append(it.path).append('|').append(it.sizeBytes).append('|').append(it.sha256).append('\n')
        }
    }

    fun checksum(): String = Checksum.sha256(canonicalText())
}
