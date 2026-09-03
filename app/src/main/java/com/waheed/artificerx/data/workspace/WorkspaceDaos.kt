package com.waheed.artificerx.data.workspace

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatThreadDao {
    @Query("SELECT * FROM chat_threads WHERE title LIKE '%' || :query || '%' ORDER BY updatedAtEpochMillis DESC LIMIT 40")
    suspend fun search(query: String): List<ChatThreadEntity>

    @Query("SELECT * FROM chat_threads WHERE archived = 0 ORDER BY updatedAtEpochMillis DESC")
    fun observeThreads(): Flow<List<ChatThreadEntity>>

    @Query("SELECT * FROM chat_threads WHERE id = :id LIMIT 1")
    suspend fun getThread(id: String): ChatThreadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(thread: ChatThreadEntity)

    @Query("UPDATE chat_threads SET title = :title, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun rename(id: String, title: String, updatedAt: Long)

    @Query("UPDATE chat_threads SET updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long)

    @Query("UPDATE chat_threads SET archived = 1, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun archive(id: String, updatedAt: Long)

    @Query("DELETE FROM chat_threads WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' ORDER BY timestampEpochMillis DESC LIMIT 40")
    suspend fun search(query: String): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestampEpochMillis ASC")
    fun observeMessages(threadId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE threadId = :threadId ORDER BY timestampEpochMillis ASC")
    suspend fun getMessages(threadId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<ChatMessageEntity>)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM chat_messages WHERE threadId = :threadId")
    suspend fun deleteForThread(threadId: String)
}

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts WHERE name LIKE '%' || :query || '%' OR mimeType LIKE '%' || :query || '%' ORDER BY createdAtEpochMillis DESC LIMIT 40")
    suspend fun search(query: String): List<ArtifactEntity>

    @Query("SELECT * FROM artifacts WHERE threadId = :threadId ORDER BY createdAtEpochMillis DESC")
    fun observeArtifacts(threadId: String): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM artifacts WHERE threadId = :threadId ORDER BY createdAtEpochMillis DESC")
    suspend fun getArtifacts(threadId: String): List<ArtifactEntity>

    @Query("SELECT * FROM artifacts ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<ArtifactEntity>>

    @Query("SELECT * FROM artifacts WHERE path = :path LIMIT 1")
    suspend fun getByPath(path: String): ArtifactEntity?

    @Query("SELECT * FROM artifacts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ArtifactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(artifact: ArtifactEntity)

    @Query("DELETE FROM artifacts WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM artifacts WHERE threadId = :threadId")
    suspend fun deleteForThread(threadId: String)
}
