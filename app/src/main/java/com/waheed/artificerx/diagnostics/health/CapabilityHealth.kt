package com.waheed.artificerx.diagnostics.health

data class CapabilityHealth(val capability: String, val available: Boolean, val latencyMs: Long?, val detail: String)
class CapabilityHealthReport(private val entries: List<CapabilityHealth>) {
    fun allHealthy(): Boolean = entries.all { it.available }
    fun blocking(): List<CapabilityHealth> = entries.filterNot { it.available }
    fun snapshot(): List<CapabilityHealth> = entries.toList()
}
