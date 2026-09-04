package com.waheed.artificerx.core.repository

import com.waheed.artificerx.data.repository.ChatWorkspaceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudioRepositoryNavigator @Inject constructor(private val repository: ChatWorkspaceRepository) {
    suspend fun searchEverything(query: String): UnifiedSearch = UnifiedSearch(
        threads = repository.searchThreads(query).map { it.id to it.title },
        messages = repository.searchMessages(query).map { it.id to it.text.take(180) },
        artifacts = repository.searchArtifacts(query).map { it.id to it.name },
    )
}
data class UnifiedSearch(val threads: List<Pair<String, String>>, val messages: List<Pair<String, String>>, val artifacts: List<Pair<String, String>>)
