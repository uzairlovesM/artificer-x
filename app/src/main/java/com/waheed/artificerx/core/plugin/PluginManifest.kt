package com.waheed.artificerx.core.plugin

data class PluginPermission(val name: String, val reason: String, val optional: Boolean = false)

data class PluginManifest(
    val id: String,
    val name: String,
    val version: Int,
    val entryPoint: String,
    val permissions: List<PluginPermission>
) {
    init {
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]{2,63}")))
        require(name.isNotBlank())
        require(version > 0)
        require(entryPoint.isNotBlank())
        require(permissions.map { it.name }.size == permissions.map { it.name }.toSet().size)
    }
}
