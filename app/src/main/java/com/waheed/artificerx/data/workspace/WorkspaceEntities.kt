package com.waheed.artificerx.data.workspace

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "chat_threads", indices = [Index("updatedAtEpochMillis")])
data class ChatThreadEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val providerId: String? = null,
    val modelId: String? = null,
    val archived: Boolean = false,
)

@Entity(tableName = "chat_messages", indices = [Index("threadId"), Index("timestampEpochMillis")])
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val role: String,
    val text: String,
    val timestampEpochMillis: Long,
    val isStreaming: Boolean,
    val attachedImageUri: String?,
    val autoSavedFileName: String?,
    val autoSavedUri: String?,
    val toolCallsJson: String,
)

@Entity(tableName = "artifacts", indices = [Index("threadId"), Index("createdAtEpochMillis")])
data class ArtifactEntity(
    @PrimaryKey val id: String,
    val threadId: String,
    val name: String,
    val mimeType: String,
    val path: String,
    val sizeBytes: Long,
    val createdAtEpochMillis: Long,
    val sourceTool: String?,
)
