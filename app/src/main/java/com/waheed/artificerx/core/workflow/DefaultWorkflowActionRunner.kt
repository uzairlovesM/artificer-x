package com.waheed.artificerx.core.workflow

import com.waheed.artificerx.core.artifact.ArtifactStore
import com.waheed.artificerx.core.insights.WorkspaceInsights
import com.waheed.artificerx.core.security.SecurityPolicy
import com.waheed.artificerx.core.terminal.TerminalSandbox
import com.waheed.artificerx.data.repository.ChatWorkspaceRepository
import com.waheed.artificerx.data.workspace.MemoryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DefaultWorkflowActionRunner @Inject constructor(
    private val artifactStore: ArtifactStore,
    private val terminalSandbox: TerminalSandbox,
    private val memoryRepository: MemoryRepository,
    private val workspaceRepository: ChatWorkspaceRepository,
) : WorkflowActionRunner {
    override suspend fun run(action: String): Result<String> {
        val command = action.trim()
        return when {
            command == "inspect_workspace" -> {
                val snapshot = WorkspaceInsights.snapshot()
                Result.success("wiring=${snapshot.wiringScore}% tools=${snapshot.toolCount} plugins=${snapshot.pluginCount}")
            }
            command.startsWith("remember:") -> {
                val payload = command.removePrefix("remember:")
                val pair = payload.split("=", limit = 2)
                if (pair.size != 2 || pair[0].isBlank()) Result.failure(IllegalArgumentException("remember:key=value required"))
                else {
                    memoryRepository.remember("workflow", pair[0], pair[1])
                    Result.success("remembered ${pair[0]}")
                }
            }
            command.startsWith("file:") -> {
                val payload = command.removePrefix("file:")
                val pair = payload.split("|", limit = 2)
                if (pair.size != 2 || pair[0].isBlank()) Result.failure(IllegalArgumentException("file:name|content required"))
                else {
                    val ref = artifactStore.writeText("workflow", pair[0], pair[1], "text/plain", "workflow")
                    Result.success("artifact=${ref.id} path=${ref.path}")
                }
            }
            command.startsWith("terminal:") -> {
                val shell = command.removePrefix("terminal:").trim()
                if (!SecurityPolicy.isShellAllowed(shell)) Result.failure(IllegalArgumentException("Command rejected by safety policy"))
                else {
                    val result = terminalSandbox.run(shell, 30)
                    if (result.exitCode == 0) Result.success(result.stdout.ifBlank { "exit=0" })
                    else Result.failure(IllegalStateException("exit=${result.exitCode}: ${result.stderr.take(800)}"))
                }
            }
            command == "list_artifacts" -> Result.success(
                workspaceRepository.observeArtifacts("workflow").first().joinToString("\n") { "${it.name} (${it.sizeBytes} bytes)" }.ifBlank { "No artifacts" },
            )
            else -> Result.failure(UnsupportedOperationException("Unknown workflow action '$command'"))
        }
    }
}
