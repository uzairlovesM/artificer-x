package com.waheed.artificerx.core.architecture

import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog
import com.waheed.artificerx.core.plugin.PluginCategory

/** Runtime feature-contract matrix. A screen is considered genuinely wired only when
 * its dependent plugin families and minimum concrete tools are present. */
object CapabilityGraph {
    data class Node(
        val id: String,
        val title: String,
        val categories: Set<PluginCategory>,
        val tools: Set<String>,
    )

    data class Report(
        val node: Node,
        val missingCategories: Set<PluginCategory>,
        val missingTools: Set<String>,
        val healthy: Boolean,
    )

    private val nodes = listOf(
        Node("chat", "Agent Chat", setOf(PluginCategory.AGENT, PluginCategory.AI_PROVIDER, PluginCategory.MODEL), setOf("finish_turn", "remember", "recall", "create_file", "create_zip", "run_terminal_command")),
        Node("creative", "Creative Studio", setOf(PluginCategory.CANVAS, PluginCategory.MEDIA, PluginCategory.EXPORT), setOf("create_layer", "draw_path", "inspect_canvas")),
        Node("sculpt", "3D Sculpt", setOf(PluginCategory.SCULPT_3D, PluginCategory.EXPORT), setOf("create_primitive", "sculpt_stroke", "inspect_scene")),
        Node("research", "Web Research", setOf(PluginCategory.WEB, PluginCategory.DOCUMENT, PluginCategory.PRODUCTIVITY), setOf("web_search", "web_fetch")),
        Node("workspace", "Project Workspace", setOf(PluginCategory.FILE, PluginCategory.DATABASE, PluginCategory.IMPORT_EXPORT, PluginCategory.SECURITY), setOf("create_file", "create_zip")),
        Node("automation", "Automation", setOf(PluginCategory.AUTOMATION, PluginCategory.AGENT, PluginCategory.FILE), setOf("run_terminal_batch", "create_file")),
        Node("media", "Media Pipeline", setOf(PluginCategory.MEDIA, PluginCategory.EXPORT), setOf("create_file", "list_artifacts", "artifact_info")),
        Node("developer", "Developer Workspace", setOf(PluginCategory.CODE, PluginCategory.FILE, PluginCategory.TERMINAL, PluginCategory.SECURITY), setOf("run_terminal_command", "run_terminal_batch", "create_file")),
        Node("memory", "Persistent Memory", setOf(PluginCategory.AGENT, PluginCategory.DATABASE, PluginCategory.SECURITY), setOf("remember", "recall", "search_workspace")),
        Node("artifacts", "Artifact Engine", setOf(PluginCategory.FILE, PluginCategory.EXPORT, PluginCategory.IMPORT_EXPORT), setOf("create_file", "create_zip", "list_artifacts", "artifact_info", "checksum_artifact")),
        Node("plugins", "Plugin Command Center", setOf(PluginCategory.OTHER, PluginCategory.AUTOMATION), setOf("get_workspace_status")),
        Node("import-export", "Import Export", setOf(PluginCategory.IMPORT_EXPORT, PluginCategory.FILE, PluginCategory.SECURITY), setOf("export_workspace_bundle", "create_zip")),
        Node("workflow", "Workflow Lab", setOf(PluginCategory.AUTOMATION, PluginCategory.AGENT, PluginCategory.FILE), setOf("run_terminal_batch", "create_file")),
        Node("security", "Security Center", setOf(PluginCategory.SECURITY, PluginCategory.TERMINAL), setOf("run_terminal_command", "get_workspace_status")),
        Node("search", "Workspace Search", setOf(PluginCategory.FILE, PluginCategory.DATABASE), setOf("search_workspace")),
        Node("diagnostics", "Runtime Diagnostics", setOf(PluginCategory.OTHER, PluginCategory.SECURITY), setOf("get_workspace_status")),
        Node("model-routing", "Model Routing", setOf(PluginCategory.AI_PROVIDER, PluginCategory.MODEL, PluginCategory.AGENT), setOf("get_workspace_status")),
        Node("local-ai", "Local AI Runtime", setOf(PluginCategory.MODEL, PluginCategory.AI_PROVIDER, PluginCategory.SECURITY), set()),
        Node("web-tools", "Web Toolchain", setOf(PluginCategory.WEB, PluginCategory.NETWORKING, PluginCategory.DOCUMENT), setOf("web_search", "web_fetch")),
        Node("creative-export", "Creative Export", setOf(PluginCategory.CANVAS, PluginCategory.EXPORT), setOf("inspect_canvas", "create_file")),
    )

    fun inspect(): List<Report> {
        val categories = BuiltinPluginCatalog.plugins.map { it.category }.toSet()
        val toolNames = ToolRegistry.ALL_TOOLS.mapNotNull { it.function?.name }.toSet()
        return nodes.map { node ->
            val missingCategories = node.categories - categories
            val missingTools = node.tools - toolNames
            Report(node, missingCategories, missingTools, missingCategories.isEmpty() && missingTools.isEmpty())
        }
    }

    fun score(): Int {
        val reports = inspect()
        if (reports.isEmpty()) return 0
        return ((reports.count { it.healthy }.toDouble() / reports.size) * 100).toInt()
    }
}
