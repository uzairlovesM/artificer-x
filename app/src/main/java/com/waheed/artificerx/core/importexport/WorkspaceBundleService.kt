package com.waheed.artificerx.core.importexport

import com.waheed.artificerx.core.artifact.ArtifactInput
import com.waheed.artificerx.core.artifact.ArtifactRef
import com.waheed.artificerx.core.artifact.ArtifactStore
import com.waheed.artificerx.core.security.SecretRedaction
import com.waheed.artificerx.data.repository.ChatWorkspaceRepository
import com.waheed.artificerx.data.workspace.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WorkspaceMemorySnapshot(val namespace: String, val key: String, val value: String, val updatedAtEpochMillis: Long)

@Serializable
data class WorkspaceBundleMeta(
    val format: Int = 1,
    val threadId: String,
    val exportedAtEpochMillis: Long,
    val messageCount: Int,
    val artifactCount: Int,
)

/** Creates a self-contained portable ZIP without exporting secrets verbatim. */
@Singleton
class WorkspaceBundleService @Inject constructor(
    private val repository: ChatWorkspaceRepository,
    private val memoryRepository: MemoryRepository,
    private val artifactStore: ArtifactStore,
) {
    private val json = Json { prettyPrint = true; encodeDefaults = true }

    suspend fun exportThread(threadId: String): ArtifactRef = withContext(Dispatchers.IO) {
        val messages = repository.loadMessages(threadId)
        val artifacts = repository.observeArtifacts(threadId).first()
        val memory = memoryRepository.list("global").take(100)
        val transcript = buildString {
            messages.forEach { message ->
                append("## ").append(message.role.name).append("\n")
                append(SecretRedaction.redact(message.text)).append("\n\n")
            }
        }
        val meta = WorkspaceBundleMeta(threadId = threadId, exportedAtEpochMillis = System.currentTimeMillis(), messageCount = messages.size, artifactCount = artifacts.size)
        val entries = buildList {
            add(ArtifactInput("workspace/meta.json", json.encodeToString(meta).toByteArray(), "application/json"))
            add(ArtifactInput("workspace/chat.md", transcript.toByteArray(), "text/markdown"))
            add(ArtifactInput("workspace/memories.json", json.encodeToString(memory.map { WorkspaceMemorySnapshot(it.namespace, it.key, SecretRedaction.redact(it.value), it.updatedAtEpochMillis) }).toByteArray(), "application/json"))
            artifacts.forEach { artifact ->
                val file = File(artifact.path)
                if (file.isFile && file.length() <= 50L * 1024L * 1024L) {
                    add(ArtifactInput("artifacts/${artifact.name}", file.readBytes(), artifact.mimeType))
                }
            }
        }
        artifactStore.writeZip(threadId, "artificerx-workspace-${threadId.take(8)}.zip", entries, "workspace_export")
    }
}
