package com.waheed.artificerx

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionRuntime

/** Owns startup orchestration signals, capability health snapshots, and safe UI/runtime hand-off state. */
class MainActivityRuntime {
    fun capabilityCount(): Int = ExpansionRuntime.all().size
    fun health(): List<CapabilityCheck> = ExpansionRuntime.health()
    fun ready(): Boolean = health().all { it.ready }
    fun areaCounts(): Map<String, Int> = ExpansionRuntime.summary()
    fun failures(): List<CapabilityCheck> = ExpansionRuntime.failures()
}
