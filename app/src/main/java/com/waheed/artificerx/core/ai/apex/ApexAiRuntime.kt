package com.waheed.artificerx.core.ai.apex

import com.waheed.artificerx.core.ai.apex.agent.AgentScheduler
import com.waheed.artificerx.core.ai.apex.kernel.ApexAgentKernel
import com.waheed.artificerx.core.ai.apex.agent.ScheduledAgent
import com.waheed.artificerx.core.ai.apex.generation.ArtifactValidator
import com.waheed.artificerx.core.ai.apex.generation.GenerationGateway
import com.waheed.artificerx.core.ai.apex.hybrid.HybridAiRuntime
import com.waheed.artificerx.core.ai.apex.hybrid.HybridRequest
import com.waheed.artificerx.core.ai.apex.model.ModelMode
import com.waheed.artificerx.core.ai.apex.stream.EventStream
import com.waheed.artificerx.core.ai.apex.verify.VerificationEngine
import com.waheed.artificerx.core.ai.apex.vision.PixelFeatureExtractor
import com.waheed.artificerx.core.ai.apex.vision.VisualCritic
import com.waheed.artificerx.core.ai.apex.vision.VisualRepairPlanner
import com.waheed.artificerx.core.ai.apex.resource.ExecutionBudget
import com.waheed.artificerx.core.ai.apex.resource.ResourceGovernor
import com.waheed.artificerx.core.ai.apex.tool.ToolAuthorizationEngine

/** End-to-end facade combining A-H capabilities without coupling them to Android UI. */
class ApexAiRuntime(private val bundle: com.waheed.artificerx.core.ai.apex.kernel.ApexRuntimeBundle) {
    private val events = EventStream()
    private val governor = ResourceGovernor(ExecutionBudget(toolCalls = 64))
    private val scheduler = AgentScheduler()
    private val critic = VisualCritic()
    private val repairs = VisualRepairPlanner()
    private val pixels = PixelFeatureExtractor()
    private val validator = ArtifactValidator()
    private val verification = VerificationEngine()
    private val auth = ToolAuthorizationEngine()

    fun analyze(prompt: String, projectId: String, revision: Long): Map<String, Any> {
        val graph = bundle.kernel.plan(prompt, projectId, revision)
        val schedule = scheduler.schedule(graph.nodes.map { ScheduledAgent(it.id, (it.cost * 100).toInt(), it.dependsOn) })
        events.emit("task.planned", mapOf("projectId" to projectId, "nodes" to graph.nodes.size.toString()))
        return mapOf("plan" to graph, "waves" to schedule.parallelWaves, "cycles" to schedule.cycles, "diagnostics" to bundle.kernel.diagnostics())
    }

    fun route(prompt: String, projectId: String, revision: Long, mode: ModelMode, vision: Boolean, tools: Boolean): com.waheed.artificerx.core.ai.apex.hybrid.HybridDecision = HybridAiRuntime(bundle.router, bundle.kernel, bundle.generation).decide(HybridRequest(prompt, mode, vision, tools, projectId, revision))
    fun observe(pixelsArgb: IntArray): com.waheed.artificerx.core.ai.apex.vision.PixelFeatures = pixels.extract(pixelsArgb)
    fun critique(before: IntArray, after: IntArray): com.waheed.artificerx.core.ai.apex.vision.CriticReport = critic.inspect(before, after)
    fun repair(report: com.waheed.artificerx.core.ai.apex.vision.CriticReport) = repairs.build(report)
    fun verify(expectedRevision: Long, actualRevision: Long, artifacts: List<String>) = verification.verify(expectedRevision, actualRevision, artifacts)
    fun authorize(request: com.waheed.artificerx.core.ai.apex.tool.ToolAuthorizationRequest) = auth.evaluate(request)
    fun generate(request: com.waheed.artificerx.core.ai.apex.generation.GenerationRequest): Result<com.waheed.artificerx.core.ai.apex.generation.GeneratedArtifact> {
        if (!governor.reserve(memory = 1L * 1024 * 1024, tools = 1)) return Result.failure(IllegalStateException("execution_budget_exhausted"))
        val result = bundle.generation.generate(request)
        result.getOrNull()?.let { artifact -> val check = validator.validate(artifact); if (!check.valid) return Result.failure(IllegalStateException(check.reasons.joinToString(","))) }
        events.emit("generation.completed", mapOf("success" to result.isSuccess.toString()))
        return result
    }
    fun events() = events.snapshot()
}
