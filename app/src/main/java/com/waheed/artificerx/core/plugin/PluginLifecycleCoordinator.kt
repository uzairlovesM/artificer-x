package com.waheed.artificerx.core.plugin

import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginLifecycleCoordinator @Inject constructor(private val manager: PluginManager) {
    suspend fun installWithDependencies(pluginId: String): Set<String> {
        val catalog = manager.catalog().associateBy { it.id }
        val visited = mutableSetOf<String>()
        suspend fun visit(id: String) {
            if (!visited.add(id)) return
            for (dep in PluginDependencyGraph.dependenciesFor(id)) {
                visit(dep)
            }
            catalog[id]?.let { manager.install(it) }
        }
        visit(pluginId)
        return visited
    }

    suspend fun enabledPluginIds(): Set<String> = manager.installed.first().filter { it.enabled }.map { it.id }.toSet()
}
