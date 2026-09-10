package com.waheed.artificerx.domain

import com.waheed.artificerx.data.local.db.ProjectEntity
import java.util.UUID

/**
 * Domain-level project descriptor retained for older callers. Room owns
 * persistence through ProjectEntity; this type deliberately contains no Room
 * annotations, preventing duplicate entity/table definitions during KSP.
 */
data class Project(
    val projectId: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = createdAt,
    val artifactPaths: List<String> = emptyList(),
) {
    fun toEntity(
        canvasWidthPx: Int = 1_024,
        canvasHeightPx: Int = 1_024,
        layersJson: String = "[]",
        activeLayerId: String? = null,
        thumbnailPath: String? = null,
    ): ProjectEntity = ProjectEntity(
        id = projectId,
        name = name,
        canvasWidthPx = canvasWidthPx,
        canvasHeightPx = canvasHeightPx,
        layersJson = layersJson,
        activeLayerId = activeLayerId,
        thumbnailPath = thumbnailPath,
        createdAtEpochMillis = createdAt,
        lastModifiedEpochMillis = lastModified,
        lastOpenedEpochMillis = null,
    )
}
