package com.waheed.artificerx.core.automation

import javax.inject.Inject
import javax.inject.Singleton

data class AiAutomationTemplate(val id: String, val name: String, val trigger: String, val steps: List<String>)
@Singleton
class AiAutomationTemplates @Inject constructor() {
    val all = listOf(
        AiAutomationTemplate("research_to_art", "Research to Artwork", "manual", listOf("web_search", "summarize_sources", "compose_scene", "inspect_canvas", "publish_artifact")),
        AiAutomationTemplate("workspace_health", "Workspace Health", "scheduled", listOf("scan_workspace", "run_static_checks", "summarize_failures", "write_report")),
        AiAutomationTemplate("model_benchmark", "Local Model Benchmark", "manual", listOf("load_model", "benchmark", "record_tokens_per_second", "compare_profiles")),
        AiAutomationTemplate("daily_archive", "Daily Archive", "scheduled", listOf("snapshot_workspace", "checksum_artifacts", "compress_bundle", "rotate_history")),
    )
}
