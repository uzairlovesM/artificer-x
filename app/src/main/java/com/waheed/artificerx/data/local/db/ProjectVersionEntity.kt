package com.waheed.artificerx.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Section 27 Version History: every meaningful checkpoint of a project
 * (agent-completed generation, manual save, pre-destructive-edit
 * snapshot) gets a row here, letting the user scrub back through the
 * project's history rather than only ever having "current state."
 */
@Entity(
    tableName = "project_versions",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class ProjectVersionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val versionLabel: String,
    val layersJson: String,
    val thumbnailPath: String?,
    val triggeredBy: String,
    val createdAtEpochMillis: Long,
)
