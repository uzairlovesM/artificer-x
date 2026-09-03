package com.waheed.artificerx.core.insights

import com.waheed.artificerx.core.architecture.CapabilityGraph
import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog

object WorkspaceInsights {
    data class Snapshot(val wiringScore: Int, val pluginCount: Int, val toolCount: Int, val healthyFeatures: Int, val totalFeatures: Int)
    fun snapshot(): Snapshot {
        val reports = CapabilityGraph.inspect()
        return Snapshot(CapabilityGraph.score(), BuiltinPluginCatalog.plugins.size, ToolRegistry.ALL_TOOLS.size, reports.count { it.healthy }, reports.size)
    }
}
