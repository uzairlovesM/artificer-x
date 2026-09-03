package com.waheed.artificerx.data.workspace

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ChatThreadEntity::class, ChatMessageEntity::class, ArtifactEntity::class, MemoryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class WorkspaceDatabase : RoomDatabase() {
    abstract fun chatThreadDao(): ChatThreadDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun artifactDao(): ArtifactDao
    abstract fun memoryDao(): MemoryDao
}
