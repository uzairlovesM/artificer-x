package com.waheed.artificerx.core.agent

import com.waheed.artificerx.data.workspace.MemoryRepository
import com.waheed.artificerx.core.storage.WorkspaceIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgentContextAssembler @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val workspaceIndex: WorkspaceIndex,
) {
    suspend fun build(userText: String, namespace: String = "global"): String = withContext(Dispatchers.IO) {
        val memories = memoryRepository.recall(namespace, userText).take(8)
        val files = workspaceIndex.scan(userText, 12)
        buildString {
            append("PERSISTENT CONTEXT\n")
            memories.forEach { append("MEMORY ${it.key}: ${it.value}\n") }
            if (files.isNotEmpty()) {
                append("RELEVANT WORKSPACE FILES\n")
                files.forEach { append("FILE ${it.relativePath} (${it.sizeBytes} bytes)\n") }
            }
        }.take(12000)
    }
}
