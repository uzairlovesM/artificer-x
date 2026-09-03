package com.waheed.artificerx.core.plugin

import kotlinx.serialization.Serializable

@Serializable
enum class PluginCategory {
    AI_PROVIDER, MODEL, IMAGE_GENERATION, CODE, FILE, EXPORT, CANVAS, SCULPT_3D, PRODUCTIVITY, WEB, AGENT, THEME_UI, IMPORT_EXPORT, DATABASE, MEDIA, AUTOMATION, SECURITY, NETWORKING, TERMINAL, DOCUMENT, OTHER,
}

@Serializable
data class PluginDescriptor(
    val id: String,
    val name: String,
    val category: PluginCategory,
    val version: String,
    val description: String,
    val enabledByDefault: Boolean = true,
    val capabilities: List<String> = emptyList(),
)

interface ArtificerPlugin {
    val descriptor: PluginDescriptor
    suspend fun onInstall() = Unit
    suspend fun onEnable() = Unit
    suspend fun onDisable() = Unit
}
