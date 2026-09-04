package com.waheed.artificerx.core.plugin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.pluginDataStore: DataStore<Preferences> by preferencesDataStore(name = "plugin_registry")

@Serializable
data class InstalledPluginRecord(val id: String, val version: String, val enabled: Boolean = true)

@Singleton
class PluginManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("installed_plugins")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    val installed: Flow<List<InstalledPluginRecord>> = context.pluginDataStore.data.map { prefs ->
        runCatching { json.decodeFromString<List<InstalledPluginRecord>>(prefs[key].orEmpty()) }.getOrDefault(emptyList())
    }

    suspend fun install(plugin: PluginDescriptor) = update(plugin.id) { InstalledPluginRecord(plugin.id, plugin.version, true) }

    suspend fun installWithDependencies(pluginId: String): Set<String> {
        val catalog = BuiltinPluginCatalog.plugins.associateBy { it.id }
        if (pluginId !in catalog) return emptySet()
        val installed = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()
        suspend fun visit(id: String) {
            if (id in installed || id in visiting) return
            val descriptor = catalog[id] ?: return
            visiting += id
            PluginDependencyGraph.dependenciesFor(id).forEach { dependency -> visit(dependency) }
            visiting -= id
            install(descriptor)
            installed += id
        }
        visit(pluginId)
        return installed
    }
    suspend fun uninstall(pluginId: String) { context.pluginDataStore.edit { prefs -> prefs[key] = json.encodeToString<List<InstalledPluginRecord>>(read(prefs).filterNot { it.id == pluginId }) } }
    suspend fun setEnabled(pluginId: String, enabled: Boolean) { context.pluginDataStore.edit { prefs -> prefs[key] = json.encodeToString<List<InstalledPluginRecord>>(read(prefs).map { if (it.id == pluginId) it.copy(enabled = enabled) else it }) } }
    fun catalog(): List<PluginDescriptor> = BuiltinPluginCatalog.plugins

    private suspend fun update(id: String, provider: (PluginDescriptor) -> InstalledPluginRecord) {
        val plugin = BuiltinPluginCatalog.plugins.firstOrNull { it.id == id } ?: return
        context.pluginDataStore.edit { prefs -> prefs[key] = json.encodeToString<List<InstalledPluginRecord>>((read(prefs).filterNot { it.id == id }) + provider(plugin)) }
    }
    private fun read(prefs: Preferences): List<InstalledPluginRecord> = runCatching { json.decodeFromString<List<InstalledPluginRecord>>(prefs[key].orEmpty()) }.getOrDefault(emptyList())
}
