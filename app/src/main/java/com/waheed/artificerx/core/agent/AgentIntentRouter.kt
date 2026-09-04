package com.waheed.artificerx.core.agent

object AgentIntentRouter {
    enum class Kind { ENGINEERING, VISUAL, RESEARCH, WORKSPACE, GENERAL }
    data class Route(val kind: Kind, val confidence: Int, val preferredTools: List<String>, val notes: String)

    fun route(prompt: String): Route {
        val p = prompt.lowercase()
        val engineering = listOf("code", "android", "kotlin", "gradle", "bug", "compile", "build", "api", "json", "github").count(p::contains)
        val visual = listOf("draw", "paint", "image", "anime", "poster", "canvas", "sculpt", "3d", "logo").count(p::contains)
        val research = listOf("research", "search", "compare", "latest", "source", "article", "website", "web").count(p::contains)
        val workspace = listOf("file", "folder", "zip", "export", "backup", "project", "document").count(p::contains)
        val winner = listOf(Kind.ENGINEERING to engineering, Kind.VISUAL to visual, Kind.RESEARCH to research, Kind.WORKSPACE to workspace).maxByOrNull { it.second }
        if (winner == null || winner.second == 0) return Route(Kind.GENERAL, 40, listOf("finish_turn", "remember", "recall"), "General reasoning route")
        val confidence = (55 + winner.second * 10).coerceAtMost(97)
        return when (winner.first) {
            Kind.ENGINEERING -> Route(winner.first, confidence, listOf("create_file", "run_terminal_command", "run_terminal_batch", "finish_turn"), "Build, inspect, test, then package")
            Kind.VISUAL -> Route(winner.first, confidence, listOf("inspect_canvas", "draw_path", "create_layer", "apply_filter", "finish_turn"), "Create, inspect and iteratively refine visual output")
            Kind.RESEARCH -> Route(winner.first, confidence, listOf("web_search", "web_fetch", "create_file", "finish_turn"), "Research first, then produce a structured artifact")
            Kind.WORKSPACE -> Route(winner.first, confidence, listOf("create_file", "create_zip", "remember", "finish_turn"), "Materialize requested files and package them")
            Kind.GENERAL -> Route(Kind.GENERAL, confidence, listOf("finish_turn"), "General reasoning route")
        }
    }
}
