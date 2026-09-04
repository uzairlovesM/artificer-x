package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.network.ToolDefinitionDto


/**
 * Prevents an unbounded runtime extension catalog from being serialized into every
 * model request. Each turn receives a deterministic, intent-focused slice plus
 * safety/continuity tools required to complete work and return real artifacts.
 */
object ToolSelectionPolicy {
    private const val MAX_TOOLS = Int.MAX_VALUE

    private val alwaysAvailable = setOf(
        "finish_turn", "remember", "recall", "create_file", "create_zip", "invoke_builtin_recipe", "search_builtin_recipes",
        "list_artifacts", "search_workspace", "artifact_info", "checksum_artifact", "workspace_status", "export_workspace_bundle",
        "inspect_canvas", "inspect_scene", "run_terminal_command", "run_terminal_batch", "install_runtime_tool",
    )

    fun select(userText: String, maxTools: Int = MAX_TOOLS): List<ToolDefinitionDto> {
        val normalized = userText.lowercase()
        val route = AgentIntentRouter.route(normalized)
        if (maxTools <= 0) return emptyList()
        val all = ToolRegistry.ALL_TOOLS
        val common = all.filter { it.function.name in alwaysAvailable || it.function.name.startsWith("runtime_") }.take(maxTools)
        val scored = all.asSequence()
            .filterNot { it.function.name in alwaysAvailable }
            .map { tool -> tool to score(tool, normalized, route.kind.name.lowercase()) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take((maxTools - common.size).coerceAtLeast(0))
            .toList()
        return (common + scored).distinctBy { it.function.name }
    }

    private fun score(tool: ToolDefinitionDto, prompt: String, family: String): Int {
        val haystack = listOf(tool.function.name, tool.function.description).joinToString(" ").lowercase()
        var score = if (haystack.contains(family)) 18 else 0
        val keywords = when (family) {
            "engineering" -> listOf("code", "file", "project", "build", "test", "json", "terminal", "git", "debug", "export", "import")
            "visual" -> listOf("canvas", "draw", "image", "color", "layer", "sculpt", "3d", "brush", "mask", "gradient", "visual")
            "research" -> listOf("web", "search", "fetch", "document", "source", "research", "url")
            "workspace" -> listOf("file", "artifact", "workspace", "folder", "history", "memory", "export", "import")
            else -> listOf("ai", "model", "chat", "agent", "file", "artifact", "memory")
        }
        keywords.forEachIndexed { index, keyword -> if (haystack.contains(keyword)) score += 8 - (index / 4) }
        if (prompt.split(Regex("\\W+")).any { it.length > 4 && haystack.contains(it) }) score += 10
        return score
    }
}
