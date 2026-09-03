package com.waheed.artificerx.core.automation

import com.waheed.artificerx.core.storage.WorkspaceFileSystem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationRepository @Inject constructor(private val fs: WorkspaceFileSystem) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val file: File get() = fs.roots.system.resolve("automations.json")

    suspend fun list(): List<AutomationRule> = withContext(Dispatchers.IO) {
        if (!file.exists()) defaultRules() else runCatching { json.decodeFromString(ListSerializer(AutomationRule.serializer()), file.readText()) }.getOrElse { defaultRules() }
    }

    suspend fun save(rules: List<AutomationRule>) = withContext(Dispatchers.IO) {
        fs.writeTextAtomic(file, json.encodeToString(ListSerializer(AutomationRule.serializer()), rules))
    }

    private fun defaultRules() = listOf(
        AutomationRule("maintenance", "Workspace maintenance", true, AutomationTrigger.DAILY, AutomationAction.CLEAN_CACHE, 24),
        AutomationRule("manifest", "Refresh workspace manifest", true, AutomationTrigger.APP_START, AutomationAction.REFRESH_MANIFEST, 0),
        AutomationRule("snapshot", "Workspace safety snapshot", false, AutomationTrigger.DAILY, AutomationAction.SNAPSHOT_WORKSPACE, 24),
    )
}
