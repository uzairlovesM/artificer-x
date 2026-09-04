package com.waheed.artificerx.runtime.extension

data class RuntimeExtension(
    val id: String,
    val version: String,
    val displayName: String,
    val capabilities: Set<String>,
    val entrypoint: String,
    val checksum: String,
    val enabled: Boolean = true
)
