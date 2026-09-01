package com.waheed.artificerx.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for a saved project (Section 27: Version History /
 * Project Save-Load). One row per project; the actual layer bitmap
 * data lives on disk as PNG files under filesDir/projects/{id}/layers/,
 * referenced here only by directory path — Room stores structured
 * metadata, not binary blobs, to keep the DB small and fast to query.
 */
@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val canvasWidthPx: Int,
    val canvasHeightPx: Int,
    val layersJson: String,
    val activeLayerId: String?,
    val thumbnailPath: String?,
    val createdAtEpochMillis: Long,
    val lastModifiedEpochMillis: Long,
    val lastOpenedEpochMillis: Long?,
)
