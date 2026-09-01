package com.waheed.artificerx.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the persisted World Model (Section "World Model /
 * persistent scene continuity"). One row per project — projectId is
 * the primary key rather than a separate auto-increment id since a
 * project has at most one World Model at a time. characters is stored
 * as a JSON-encoded string (see WorldModelStore) rather than a
 * separate joined table since it's small, always read/written as a
 * whole with the rest of the World Model, and never queried on its
 * own — a join would add complexity with no real benefit here.
 */
@Entity(tableName = "world_models")
data class WorldModelEntity(
    @PrimaryKey val projectId: String,
    val establishedStyle: String?,
    val establishedPalette: String?,
    val establishedSetting: String?,
    val charactersJson: String,
    val additionalNotes: String?,
    val lastUpdatedEpochMillis: Long,
)
