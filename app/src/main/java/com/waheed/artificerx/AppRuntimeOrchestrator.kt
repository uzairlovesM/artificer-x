package com.waheed.artificerx

import com.waheed.artificerx.core.expansion.CapabilityCheck
import com.waheed.artificerx.core.expansion.ExpansionRuntime

/** Coordinates expansion health, boot telemetry, and lifecycle-safe capability readiness. */
class AppRuntimeOrchestrator {
    fun capabilityCount(): Int = ExpansionRuntime.all().size
    fun health(): List<CapabilityCheck> = ExpansionRuntime.health()
    fun ready(): Boolean = health().all { it.ready }
    fun areaCounts(): Map<String, Int> = ExpansionRuntime.summary()
    fun failures(): List<CapabilityCheck> = ExpansionRuntime.failures()
}
