package com.waheed.artificerx.core.diagnostics

import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog
import com.waheed.artificerx.core.plugin.PluginCategory

data class FeatureAuditItem(
    val id: String,
    val title: String,
    val expectedCapabilities: Set<PluginCategory>,
    val actualCapabilities: Set<PluginCategory>,
    val missingCapabilities: Set<PluginCategory>,
    val missingTools: Set<String> = emptySet(),
    val status: AuditStatus,
)

enum class AuditStatus { HEALTHY, PARTIAL, AT_RISK }

object FeatureWiringAudit {
    private val featureExpectations = linkedMapOf(
        "agent-chat" to setOf(PluginCategory.AGENT, PluginCategory.AI_PROVIDER, PluginCategory.MODEL, PluginCategory.FILE, PluginCategory.EXPORT, PluginCategory.PRODUCTIVITY),
        "creative-studio" to setOf(PluginCategory.CANVAS, PluginCategory.IMAGE_GENERATION, PluginCategory.MEDIA, PluginCategory.EXPORT, PluginCategory.AUTOMATION),
        "sculpt-studio" to setOf(PluginCategory.SCULPT_3D, PluginCategory.EXPORT, PluginCategory.MEDIA),
        "web-research" to setOf(PluginCategory.WEB, PluginCategory.DOCUMENT, PluginCategory.PRODUCTIVITY),
        "project-workspace" to setOf(PluginCategory.FILE, PluginCategory.DATABASE, PluginCategory.IMPORT_EXPORT, PluginCategory.SECURITY),
        "automation" to setOf(PluginCategory.AUTOMATION, PluginCategory.AGENT, PluginCategory.FILE),
    )

    fun run(): List<FeatureAuditItem> {
        val installedCategories = BuiltinPluginCatalog.plugins.map { it.category }.toSet()
        val toolCount = ToolRegistry.ALL_TOOLS.size
        val toolNames = ToolRegistry.ALL_TOOLS.map { it.function.name }.toSet()
        val graph = com.waheed.artificerx.core.architecture.CapabilityGraph.inspect().associateBy { it.node.id }
        return featureExpectations.map { (id, expected) ->
            val missing = expected - installedCategories
            val missingTools = graph[id]?.missingTools.orEmpty()
            val status = when {
                missing.isNotEmpty() || missingTools.isNotEmpty() -> AuditStatus.AT_RISK
                toolNames.size < 100 -> AuditStatus.PARTIAL
                else -> AuditStatus.HEALTHY
            }
            FeatureAuditItem(id, id.replace('-', ' ').replaceFirstChar { it.uppercase() }, expected, installedCategories, missing, missingTools, status)
        }
    }
}
