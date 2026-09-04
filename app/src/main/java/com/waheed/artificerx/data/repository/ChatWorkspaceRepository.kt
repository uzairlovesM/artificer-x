package com.waheed.artificerx.data.repository

import android.net.Uri
import com.waheed.artificerx.data.workspace.ArtifactEntity
import com.waheed.artificerx.data.workspace.ChatMessageDao
import com.waheed.artificerx.data.workspace.ChatMessageEntity
import com.waheed.artificerx.data.workspace.ChatThreadDao
import com.waheed.artificerx.data.workspace.ChatThreadEntity
import com.waheed.artificerx.domain.model.ChatMessage
import com.waheed.artificerx.domain.model.ChatMessageRole
import com.waheed.artificerx.domain.model.ToolCallEntry
import com.waheed.artificerx.domain.model.ToolCallStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class StoredToolCall(
    val id: String,
    val toolName: String,
    val argsPreview: String,
    val status: String,
    val resultSummary: String? = null,
)

@Singleton
class ChatWorkspaceRepository @Inject constructor(
    private val threadDao: ChatThreadDao,
    private val messageDao: ChatMessageDao,
    private val artifactDao: com.waheed.artificerx.data.workspace.ArtifactDao,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun observeThreads(): Flow<List<ChatThreadEntity>> = threadDao.observeThreads()
    fun observeMessages(threadId: String): Flow<List<ChatMessageEntity>> = messageDao.observeMessages(threadId)
    fun observeArtifacts(threadId: String): Flow<List<ArtifactEntity>> = artifactDao.observeArtifacts(threadId)
    fun observeAllArtifacts(): Flow<List<ArtifactEntity>> = artifactDao.observeAll()

    suspend fun searchThreads(query: String) = threadDao.search(query.trim().take(100))
    suspend fun searchMessages(query: String) = messageDao.search(query.trim().take(100))
    suspend fun searchArtifacts(query: String) = artifactDao.search(query.trim().take(100))
    suspend fun getArtifact(id: String): ArtifactEntity? = artifactDao.getById(id)

    suspend fun deleteArtifact(id: String) {
        artifactDao.getById(id)?.let { artifact ->
            runCatching { java.io.File(artifact.path).delete() }
            artifactDao.delete(id)
        }
    }

    suspend fun deleteArtifactAtPath(path: String) {
        artifactDao.getByPath(path)?.let { deleteArtifact(it.id) } ?: runCatching { java.io.File(path).delete() }
    }

    suspend fun ensureThread(id: String, title: String = "New conversation") {
        if (threadDao.getThread(id) == null) {
            val now = System.currentTimeMillis()
            threadDao.upsert(ChatThreadEntity(id, title, now, now))
        }
    }

    suspend fun createThread(title: String = "New conversation"): String {
        val id = java.util.UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        threadDao.upsert(ChatThreadEntity(id, title, now, now))
        return id
    }

    suspend fun renameThread(id: String, title: String) = threadDao.rename(id, title.trim().ifBlank { "New conversation" }, System.currentTimeMillis())
    suspend fun archiveThread(id: String) = threadDao.archive(id, System.currentTimeMillis())

    /** Permanently removes a conversation and its database rows. Artifact files are cleaned best-effort. */
    suspend fun deleteThreadForever(id: String) {
        val artifacts = artifactDao.getArtifacts(id)
        artifacts.forEach { runCatching { java.io.File(it.path).delete() } }
        messageDao.deleteForThread(id)
        artifactDao.deleteForThread(id)
        threadDao.delete(id)
    }

    suspend fun saveMessage(threadId: String, message: ChatMessage) {
        ensureThread(threadId)
        val storedCalls = message.toolCalls.map {
            StoredToolCall(it.id, it.toolName, it.argsPreview, it.status.name, it.resultSummary)
        }
        messageDao.upsert(
            ChatMessageEntity(
                id = message.id,
                threadId = threadId,
                role = message.role.name,
                text = message.text,
                timestampEpochMillis = message.timestampEpochMillis,
                isStreaming = message.isStreaming,
                attachedImageUri = message.attachedImageUri,
                autoSavedFileName = message.autoSavedFileName,
                autoSavedUri = message.autoSavedUri?.toString(),
                toolCallsJson = json.encodeToString<List<StoredToolCall>>(storedCalls),
            ),
        )
        threadDao.touch(threadId, System.currentTimeMillis())
    }

    suspend fun loadMessages(threadId: String): List<ChatMessage> = messageDao.getMessages(threadId).map { it.toDomain(json) }

    suspend fun saveArtifact(artifact: ArtifactEntity) {
        artifactDao.upsert(artifact)
        threadDao.touch(artifact.threadId, System.currentTimeMillis())
    }

    private fun ChatMessageEntity.toDomain(json: Json): ChatMessage {
        val calls = runCatching { json.decodeFromString<List<StoredToolCall>>(toolCallsJson) }.getOrDefault(emptyList())
        return ChatMessage(
            id = id,
            role = runCatching { ChatMessageRole.valueOf(role) }.getOrDefault(ChatMessageRole.AGENT),
            text = text,
            toolCalls = calls.map {
                ToolCallEntry(
                    id = it.id,
                    toolName = it.toolName,
                    argsPreview = it.argsPreview,
                    status = runCatching { ToolCallStatus.valueOf(it.status) }.getOrDefault(ToolCallStatus.FAILED),
                    resultSummary = it.resultSummary,
                )
            },
            timestampEpochMillis = timestampEpochMillis,
            isStreaming = isStreaming,
            attachedImageUri = attachedImageUri,
            autoSavedFileName = autoSavedFileName,
            autoSavedUri = autoSavedUri?.let(Uri::parse),
        )
    }
}
