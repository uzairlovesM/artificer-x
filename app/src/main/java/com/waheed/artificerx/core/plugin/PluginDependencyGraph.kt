package com.waheed.artificerx.core.plugin

object PluginDependencyGraph {
    private val dependencies = mapOf(
        "image_generation.image.generation" to setOf("ai_provider.openai.compatible", "model.model.router"),
        "image_generation.batch.actions" to setOf("image_generation.image.generation", "automation.batch.actions"),
        "code.code.workspace" to setOf("file.file.explorer", "other.diagnostics"),
        "code.tests.runner" to setOf("code.code.workspace", "code.build.diagnostics"),
        "web.web.search" to setOf("document.document.handler"),
        "web.html.fetcher" to setOf("web.web.search", "document.document.handler"),
        "automation.automation.engine" to setOf("agent.agent.runtime", "file.file.explorer"),
        "export.export.bundle" to setOf("file.file.explorer", "document.document.packager"),
        "security.sandbox" to setOf("security.permission.gate", "security.secret.redaction"),
    )

    fun dependenciesFor(pluginId: String): Set<String> = dependencies[pluginId].orEmpty()

    /** Detects catalog-level missing dependencies without conflating availability with installation state. */
    fun missingFromCatalog(catalogIds: Set<String>): Map<String, Set<String>> = dependencies.mapNotNull { (id, deps) ->
        val missing = deps - catalogIds
        if (missing.isEmpty()) null else id to missing
    }.toMap()

    fun missing(installedIds: Set<String>): Map<String, Set<String>> = dependencies.mapNotNull { (id, deps) ->
        val missing = deps - installedIds
        if (missing.isEmpty()) null else id to missing
    }.toMap()

    fun cycles(): List<List<String>> {
        val cycles = mutableListOf<List<String>>()
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        fun visit(node: String, path: List<String>) {
            if (node in visiting) {
                val index = path.indexOf(node)
                if (index >= 0) cycles += path.drop(index) + node
                return
            }
            if (!visited.add(node)) return
            visiting += node
            dependencies[node].orEmpty().forEach { visit(it, path + it) }
            visiting -= node
        }
        dependencies.keys.forEach { visit(it, listOf(it)) }
        return cycles.map { it.distinct() }.distinct()
    }
}
