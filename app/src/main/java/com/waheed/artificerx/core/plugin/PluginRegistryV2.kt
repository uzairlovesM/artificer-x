package com.waheed.artificerx.core.plugin

class PluginRegistryV2 {
    private val plugins = LinkedHashMap<String, PluginManifest>()

    @Synchronized
    fun install(manifest: PluginManifest): Boolean {
        val existing = plugins[manifest.id]
        if (existing != null && existing.version >= manifest.version) return false
        plugins[manifest.id] = manifest
        return true
    }

    @Synchronized fun uninstall(id: String): Boolean = plugins.remove(id) != null
    @Synchronized fun get(id: String): PluginManifest? = plugins[id]
    @Synchronized fun all(): List<PluginManifest> = plugins.values.toList()
}
