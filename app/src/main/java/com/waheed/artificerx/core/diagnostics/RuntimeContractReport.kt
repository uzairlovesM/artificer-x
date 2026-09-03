package com.waheed.artificerx.core.diagnostics

import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog
import com.waheed.artificerx.core.plugin.PluginDependencyGraph

data class RuntimeContractReport(
    val duplicateToolNames: List<String>,
    val duplicatePluginIds: List<String>,
    val dependencyGaps: Map<String, Set<String>>,
    val dependencyCycles: List<List<String>>,
    val toolCount: Int,
    val pluginCount: Int,
) {
    val healthy: Boolean get() = duplicateToolNames.isEmpty() && duplicatePluginIds.isEmpty() && dependencyGaps.isEmpty() && dependencyCycles.isEmpty()
}

object RuntimeContractInspector {
    fun run(): RuntimeContractReport {
        val toolNames = ToolRegistry.ALL_TOOLS.map { it.function.name }
        val pluginIds = BuiltinPluginCatalog.plugins.map { it.id }
        return RuntimeContractReport(
            duplicateToolNames = toolNames.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted(),
            duplicatePluginIds = pluginIds.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted(),
            dependencyGaps = PluginDependencyGraph.missingFromCatalog(pluginIds.toSet()),
            dependencyCycles = PluginDependencyGraph.cycles(),
            toolCount = toolNames.size,
            pluginCount = pluginIds.size,
        )
    }
}
