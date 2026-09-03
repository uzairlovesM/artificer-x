package com.waheed.artificerx.core.automation

import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import com.waheed.artificerx.core.storage.WorkspaceManifestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationEngine @Inject constructor(
    private val repository: AutomationRepository,
    private val fs: WorkspaceFileSystem,
    private val manifestService: WorkspaceManifestService,
) {
    suspend fun run(rule: AutomationRule): String = withContext(Dispatchers.IO) {
        when (rule.action) {
            AutomationAction.CLEAN_CACHE -> "Cleared ${fs.clearCache()} bytes of cache"
            AutomationAction.REFRESH_MANIFEST -> { manifestService.refresh(); "Workspace manifest refreshed" }
            AutomationAction.VERIFY_ARTIFACTS -> {
                val count = fs.listFiles(fs.roots.root).count(); "Verified $count managed files"
            }
            AutomationAction.SNAPSHOT_WORKSPACE -> {
                val target = fs.roots.backups.resolve("snapshot-${Instant.now().toEpochMilli()}.json")
                fs.writeTextAtomic(target, "{\"createdAt\":${System.currentTimeMillis()},\"usageBytes\":${fs.usageBytes()}}")
                "Snapshot created: ${target.name}"
            }
        }
    }

    suspend fun runDue(trigger: AutomationTrigger): List<String> = repository.list().filter { it.enabled && it.trigger == trigger }.map { "${it.name}: ${run(it)}" }
}
