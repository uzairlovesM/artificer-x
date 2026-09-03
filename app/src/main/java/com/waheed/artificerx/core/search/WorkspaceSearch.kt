package com.waheed.artificerx.core.search

import com.waheed.artificerx.data.repository.ChatWorkspaceRepository
import javax.inject.Inject
import javax.inject.Singleton

data class WorkspaceSearchResult(val kind: String, val id: String, val title: String, val subtitle: String)

@Singleton
class WorkspaceSearch @Inject constructor(private val repository: ChatWorkspaceRepository) {
    suspend fun search(query: String): List<WorkspaceSearchResult> {
        val q = query.trim().take(100)
        if (q.isBlank()) return emptyList()
        val threads = repository.searchThreads(q).map { WorkspaceSearchResult("chat", it.id, it.title, "Conversation") }
        val messages = repository.searchMessages(q).map { WorkspaceSearchResult("message", it.id, it.text.take(72).ifBlank { "Message" }, "Message in ${it.threadId.take(8)}") }
        val artifacts = repository.searchArtifacts(q).map { WorkspaceSearchResult("artifact", it.id, it.name, "${it.mimeType} • ${it.sizeBytes} bytes") }
        return (threads + messages + artifacts).distinctBy { it.kind + ":" + it.id }.take(60)
    }
}
