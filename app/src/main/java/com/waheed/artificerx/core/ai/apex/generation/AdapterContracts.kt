package com.waheed.artificerx.core.ai.apex.generation

enum class AdapterKind { LOCAL, REMOTE, IMAGE_GENERATION, VISION, EMBEDDING }
data class AdapterHealth(val id: String, val kind: AdapterKind, val healthy: Boolean, val latencyMs: Long, val message: String = "")
interface HealthAwareAdapter { val health: AdapterHealth }
