package com.waheed.artificerx.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, ProjectVersionEntity::class, WorldModelEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class ArtificerXDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun projectVersionDao(): ProjectVersionDao

    abstract fun worldModelDao(): WorldModelDao
}
