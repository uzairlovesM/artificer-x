package com.waheed.artificerx.core.automation

import kotlinx.serialization.Serializable

@Serializable
enum class AutomationTrigger { APP_START, DAILY, AFTER_ARTIFACT, MANUAL }

@Serializable
enum class AutomationAction { CLEAN_CACHE, REFRESH_MANIFEST, VERIFY_ARTIFACTS, SNAPSHOT_WORKSPACE }

@Serializable
data class AutomationRule(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val trigger: AutomationTrigger,
    val action: AutomationAction,
    val intervalHours: Long = 24,
)
