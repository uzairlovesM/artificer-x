package com.waheed.artificerx.core.expansion

/** Unified runtime view over all generated capability indexes. */
object ExpansionRuntime {
    fun all(): List<ExpansionCapability> = GeneratedExpansionIndex.all + GeneratedSecondaryExpansionIndex.all
    fun health(): List<CapabilityCheck> = all().map { it.validate() }
    fun byArea(area: String): List<ExpansionCapability> = all().filter { it.area.equals(area, ignoreCase = true) }
    fun summary(): Map<String, Int> = all().groupingBy { it.area }.eachCount()
    fun failures(): List<CapabilityCheck> = health().filterNot { it.ready }
    fun readyCount(): Int = health().count { it.ready }
}
