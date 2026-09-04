package com.waheed.artificerx.core.usecase

import com.waheed.artificerx.core.automation.AiAutomationTemplates
import com.waheed.artificerx.core.agent.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceCapabilityUseCase @Inject constructor(private val automations:AiAutomationTemplates) {
    fun snapshot() = mapOf("tools" to ToolRegistry.ALL_TOOLS.size, "automation_templates" to automations.all.size, "native" to 1, "runtime_extensions" to 1)
}
