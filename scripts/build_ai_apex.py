from pathlib import Path
root = Path('/mnt/data/artificer_major_all/app/src/main/java/com/waheed/artificerx/core/ai/apex')
files = {
'kernel/ApexAgentKernel.kt': r'''package com.waheed.artificerx.core.ai.apex.kernel

import com.waheed.artificerx.core.ai.apex.agent.AgentTask
import com.waheed.artificerx.core.ai.apex.agent.AgentTaskGraph
import com.waheed.artificerx.core.ai.apex.agent.TaskNode
import com.waheed.artificerx.core.ai.apex.memory.CreativeMemoryStore
import com.waheed.artificerx.core.ai.apex.model.ModelRouter
import com.waheed.artificerx.core.ai.apex.tool.ToolRuntime
import com.waheed.artificerx.core.ai.apex.verify.VerificationEngine
import com.waheed.artificerx.core.ai.apex.vision.CanvasWorldModel
import com.waheed.artificerx.core.ai.apex.vision.VisualCritic

/** Provider-neutral agent kernel for the complete A-H runtime. */
class ApexAgentKernel(
    private val router: ModelRouter,
    private val tools: ToolRuntime,
    private val memory: CreativeMemoryStore,
    private val world: CanvasWorldModel,
    private val critic: VisualCritic,
    private val verifier: VerificationEngine,
) {
    fun plan(prompt: String, projectId: String, canvasRevision: Long): AgentTaskGraph {
        require(prompt.isNotBlank())
        val intent = prompt.trim()
        val nodes = mutableListOf<TaskNode>()
        nodes += TaskNode("inspect", "Inspect project and canvas state", emptySet(), 1.0)
        nodes += TaskNode("memory", "Retrieve relevant creative memory", setOf("inspect"), 0.7)
        if (intent.contains("draw", true) || intent.contains("edit", true) || intent.contains("change", true)) {
            nodes += TaskNode("plan_canvas", "Compile safe canvas operations", setOf("inspect", "memory"), 1.4)
            nodes += TaskNode("execute_canvas", "Execute approved canvas operations", setOf("plan_canvas"), 2.0)
            nodes += TaskNode("verify_visual", "Critique result against the requested intent", setOf("execute_canvas"), 1.5)
        } else if (intent.contains("research", true) || intent.contains("search", true)) {
            nodes += TaskNode("research", "Collect evidence and rank sources", setOf("inspect", "memory"), 2.0)
        } else {
            nodes += TaskNode("respond", "Generate a grounded response", setOf("inspect", "memory"), 1.0)
        }
        nodes += TaskNode("commit", "Commit verified artifacts and memory", nodes.map { it.id }.toSet(), 0.6)
        return AgentTaskGraph(AgentTask(projectId, canvasRevision, intent), nodes)
    }

    fun diagnostics(): Map<String, Any> = mapOf(
        "routerProviders" to router.providerCount(),
        "tools" to tools.count(),
        "memoryEntries" to memory.size(),
        "worldRevision" to world.revision(),
        "criticReady" to critic.ready(),
        "verifierReady" to verifier.ready(),
    )
}
''',
'agent/AgentTaskGraph.kt': r'''package com.waheed.artificerx.core.ai.apex.agent

data class AgentTask(val projectId: String, val canvasRevision: Long, val prompt: String)
data class TaskNode(val id: String, val description: String, val dependsOn: Set<String>, val cost: Double)

data class AgentTaskGraph(val task: AgentTask, val nodes: List<TaskNode>) {
    init {
        require(task.projectId.isNotBlank())
        require(task.prompt.isNotBlank())
        require(nodes.isNotEmpty())
        val ids = nodes.map { it.id }.toSet()
        require(ids.size == nodes.size)
        nodes.forEach { require(it.dependsOn.all(ids::contains)) }
    }
    fun ready(completed: Set<String>): List<TaskNode> = nodes.filter { it.id !in completed && it.dependsOn.all(completed::contains) }
    fun totalCost(): Double = nodes.sumOf { it.cost }
}
''',
'agent/MultiAgentSupervisor.kt': r'''package com.waheed.artificerx.core.ai.apex.agent

enum class SpecialistRole { SUPERVISOR, ARTIST, VISION, COLOR, RESEARCH, LAYOUT, ANIMATION, CRITIC, ARCHIVIST }

data class SpecialistProposal(val role: SpecialistRole, val summary: String, val confidence: Float, val dependencies: Set<SpecialistRole> = emptySet())

data class SupervisorDecision(val accepted: List<SpecialistProposal>, val rejected: List<SpecialistProposal>, val reason: String)

class MultiAgentSupervisor {
    fun decide(proposals: List<SpecialistProposal>): SupervisorDecision {
        val accepted = proposals
            .filter { it.confidence >= 0.6f }
            .sortedByDescending { it.confidence }
            .distinctBy { it.role }
        val acceptedRoles = accepted.map { it.role }.toSet()
        val resolved = accepted.filter { it.dependencies.all(acceptedRoles::contains) }
        val rejected = proposals.filterNot { it in resolved }
        return SupervisorDecision(resolved, rejected, "Deterministic confidence + dependency arbitration")
    }
}
''',
'context/ContextEngine.kt': r'''package com.waheed.artificerx.core.ai.apex

data class ContextItem(val id: String, val kind: String, val text: String, val relevance: Double, val priority: Int)

data class ContextPack(val items: List<ContextItem>, val estimatedTokens: Int) {
    fun asPrompt(): String = items.joinToString("\n") { "[${it.kind}:${it.id}] ${it.text}" }
}

class ContextEngine {
    fun compile(items: Collection<ContextItem>, maxTokens: Int): ContextPack {
        require(maxTokens > 0)
        var used = 0
        val selected = items.sortedWith(compareByDescending<ContextItem> { it.priority }.thenByDescending { it.relevance })
            .filter {
                val cost = estimateTokens(it.text)
                if (used + cost <= maxTokens) { used += cost; true } else false
            }
        return ContextPack(selected, used)
    }

    private fun estimateTokens(text: String): Int = ((text.trim().length + 3) / 4).coerceAtLeast(1)
}
''',
'model/ModelRouter.kt': r'''package com.waheed.artificerx.core.ai.apex.model

enum class ModelMode { OFFLINE, FAST, QUALITY, VISION, REASONING, HYBRID }

data class ModelEndpoint(val id: String, val local: Boolean, val supportsVision: Boolean, val supportsTools: Boolean, val maxContextTokens: Int, val health: Double = 1.0)

data class ModelSelection(val endpoint: ModelEndpoint, val score: Double, val reasons: List<String>)

class ModelRouter(private val endpoints: MutableList<ModelEndpoint> = mutableListOf()) {
    fun register(endpoint: ModelEndpoint) { require(endpoint.id.isNotBlank()); endpoints.removeAll { it.id == endpoint.id }; endpoints += endpoint }
    fun providerCount(): Int = endpoints.size
    fun select(mode: ModelMode, needsVision: Boolean, needsTools: Boolean, minContext: Int = 4_096): ModelSelection? {
        val candidates = endpoints.filter { it.maxContextTokens >= minContext && (!needsVision || it.supportsVision) && (!needsTools || it.supportsTools) }
        return candidates.map { endpoint ->
            var score = endpoint.health * 100.0
            val reasons = mutableListOf<String>()
            if (mode == ModelMode.OFFLINE && endpoint.local) { score += 30; reasons += "local" }
            if (mode != ModelMode.OFFLINE && !endpoint.local) { score += 10; reasons += "remote" }
            if (needsVision && endpoint.supportsVision) { score += 20; reasons += "vision" }
            if (needsTools && endpoint.supportsTools) { score += 20; reasons += "tools" }
            if (endpoint.maxContextTokens >= minContext * 2) { score += 5; reasons += "context" }
            ModelSelection(endpoint, score, reasons)
        }.maxByOrNull { it.score }
    }
}
''',
'memory/CreativeMemoryStore.kt': r'''package com.waheed.artificerx.core.ai.apex.memory

import java.security.MessageDigest

enum class MemoryScope { GLOBAL, PROJECT, CHAT, CHARACTER, STYLE, BRUSH, PALETTE }

data class CreativeMemory(val id: String, val scope: MemoryScope, val text: String, val weight: Double, val createdAt: Long, var lastUsedAt: Long = createdAt, var uses: Long = 0)

class CreativeMemoryStore(private val capacity: Int = 5_000) {
    private val entries = LinkedHashMap<String, CreativeMemory>()
    @Synchronized fun remember(scope: MemoryScope, text: String, weight: Double = 0.5, now: Long = System.currentTimeMillis()): CreativeMemory {
        require(text.isNotBlank()); require(weight in 0.0..1.0)
        val id = digest(scope.name + "|" + text.trim())
        val current = entries[id]
        val value = if (current == null) CreativeMemory(id, scope, text.trim(), weight, now) else current.copy(weight = maxOf(current.weight, weight), lastUsedAt = now, uses = current.uses + 1)
        entries[id] = value
        trim()
        return value
    }
    @Synchronized fun recall(query: String, scope: MemoryScope? = null, limit: Int = 12): List<CreativeMemory> {
        val q = query.lowercase().split(Regex("\\W+")).filter(String::isNotBlank).toSet()
        return entries.values.asSequence().filter { scope == null || it.scope == scope }.map { memory ->
            val words = memory.text.lowercase().split(Regex("\\W+")).toSet()
            val overlap = if (q.isEmpty()) 0.0 else q.intersect(words).size.toDouble() / q.size
            Triple(memory, overlap + memory.weight * 0.25 + (memory.uses.coerceAtMost(20) / 100.0), overlap)
        }.filter { it.third > 0.0 || it.first.weight >= 0.8 }.sortedByDescending { it.second }.take(limit).map { it.first.also { it.lastUsedAt = System.currentTimeMillis(); it.uses++ } }.toList()
    }
    @Synchronized fun forget(id: String): Boolean = entries.remove(id) != null
    @Synchronized fun decay(now: Long = System.currentTimeMillis(), halfLifeDays: Double = 30.0) { entries.entries.forEach { (id, memory) -> val ageDays = (now - memory.lastUsedAt).coerceAtLeast(0L) / 86_400_000.0; val factor = Math.pow(0.5, ageDays / halfLifeDays); if (memory.weight * factor < 0.05) entries.remove(id) } }
    @Synchronized fun size(): Int = entries.size
    private fun trim() { while (entries.size > capacity) entries.remove(entries.values.minByOrNull { it.weight + it.uses / 100.0 }?.id) }
    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
''',
'vision/CanvasWorldModel.kt': r'''package com.waheed.artificerx.core.ai.apex.vision

data class SceneRegion(val id: String, val left: Float, val top: Float, val right: Float, val bottom: Float, val semantic: String, val confidence: Float) {
    init { require(right >= left && bottom >= top); require(confidence in 0f..1f) }
}

data class CanvasWorldSnapshot(val revision: Long, val width: Int, val height: Int, val regions: List<SceneRegion>, val paletteArgb: List<Int>, val metadata: Map<String, String>)

class CanvasWorldModel {
    private var value = CanvasWorldSnapshot(0L, 1, 1, emptyList(), emptyList(), emptyMap())
    @Synchronized fun update(snapshot: CanvasWorldSnapshot) { require(snapshot.revision >= value.revision); value = snapshot }
    @Synchronized fun snapshot(): CanvasWorldSnapshot = value
    @Synchronized fun revision(): Long = value.revision
    fun findRegion(label: String): SceneRegion? = snapshot().regions.filter { it.semantic.contains(label, true) }.maxByOrNull { it.confidence }
}
''',
'vision/VisualDiffEngine.kt': r'''package com.waheed.artificerx.core.ai.apex.vision

import kotlin.math.abs

data class VisualDiffMetrics(val meanAbsoluteError: Double, val changedPixels: Long, val totalPixels: Long, val changedRatio: Double, val similarity: Double)

class VisualDiffEngine {
    fun compare(before: IntArray, after: IntArray): VisualDiffMetrics {
        require(before.size == after.size)
        if (before.isEmpty()) return VisualDiffMetrics(0.0, 0, 0, 0.0, 1.0)
        var error = 0.0
        var changed = 0L
        for (i in before.indices) {
            val a = before[i]; val b = after[i]
            val dr = abs(((a ushr 16) and 0xff) - ((b ushr 16) and 0xff))
            val dg = abs(((a ushr 8) and 0xff) - ((b ushr 8) and 0xff))
            val db = abs((a and 0xff) - (b and 0xff))
            val da = abs(((a ushr 24) and 0xff) - ((b ushr 24) and 0xff))
            val d = (dr + dg + db + da) / 1020.0
            error += d
            if (d > 0.01) changed++
        }
        val ratio = changed.toDouble() / before.size
        val mae = error / before.size
        return VisualDiffMetrics(mae, changed, before.size.toLong(), ratio, (1.0 - mae).coerceIn(0.0, 1.0))
    }
}
''',
'vision/VisualCritic.kt': r'''package com.waheed.artificerx.core.ai.apex.vision

data class CriticIssue(val code: String, val message: String, val severity: Double)
data class CriticReport(val score: Double, val issues: List<CriticIssue>, val pass: Boolean)

class VisualCritic(private val diff: VisualDiffEngine = VisualDiffEngine()) {
    fun ready(): Boolean = true
    fun inspect(before: IntArray, after: IntArray, expectedChangeRatio: ClosedFloatingPointRange<Double>? = null): CriticReport {
        val metrics = diff.compare(before, after)
        val issues = mutableListOf<CriticIssue>()
        if (metrics.changedRatio < 0.00001 && expectedChangeRatio?.start ?: 0.0 > 0.0) issues += CriticIssue("NO_VISIBLE_CHANGE", "Expected a visible canvas change but the result is effectively identical.", 0.8)
        if (expectedChangeRatio != null && metrics.changedRatio !in expectedChangeRatio) issues += CriticIssue("CHANGE_SCOPE", "Visible change ratio is outside the requested scope.", 0.6)
        val score = (metrics.similarity - issues.sumOf { it.severity } * 0.2).coerceIn(0.0, 1.0)
        return CriticReport(score, issues, issues.none { it.severity >= 0.7 })
    }
}
''',
'verify/VerificationEngine.kt': r'''package com.waheed.artificerx.core.ai.apex.verify

data class VerificationCheck(val id: String, val passed: Boolean, val details: String)
data class VerificationReport(val passed: Boolean, val checks: List<VerificationCheck>)

class VerificationEngine {
    fun ready(): Boolean = true
    fun verify(expectedRevision: Long, actualRevision: Long, artifacts: List<String>, requiresArtifact: Boolean = false): VerificationReport {
        val checks = listOf(
            VerificationCheck("revision", actualRevision >= expectedRevision, "actual=$actualRevision expectedAtLeast=$expectedRevision"),
            VerificationCheck("artifacts", !requiresArtifact || artifacts.any(String::isNotBlank), "artifactCount=${artifacts.size}"),
            VerificationCheck("finite", expectedRevision >= 0 && actualRevision >= 0, "revision domain valid"),
        )
        return VerificationReport(checks.all { it.passed }, checks)
    }
}
''',
'tool/ToolRuntime.kt': r'''package com.waheed.artificerx.core.ai.apex.tool

import java.util.concurrent.atomic.AtomicLong

data class ToolSpec(val id: String, val version: Int, val dangerous: Boolean, val idempotent: Boolean, val timeoutMs: Long = 30_000L)
data class ToolRequest(val toolId: String, val input: Map<String, String>, val correlationId: String)
data class ToolResult(val success: Boolean, val output: Map<String, String>, val error: String? = null, val elapsedMs: Long = 0L)

class ToolRuntime {
    private val specs = LinkedHashMap<String, ToolSpec>()
    private val executions = AtomicLong(0)
    @Synchronized fun register(spec: ToolSpec) { require(spec.id.isNotBlank()); specs[spec.id] = spec }
    @Synchronized fun count(): Int = specs.size
    @Synchronized fun spec(id: String): ToolSpec? = specs[id]
    fun execute(request: ToolRequest, handler: (ToolRequest) -> Map<String, String>): ToolResult {
        val spec = spec(request.toolId) ?: return ToolResult(false, emptyMap(), "unknown_tool")
        val started = System.nanoTime()
        return try {
            val out = handler(request)
            executions.incrementAndGet()
            val elapsed = (System.nanoTime() - started) / 1_000_000
            if (elapsed > spec.timeoutMs) ToolResult(false, emptyMap(), "timeout", elapsed) else ToolResult(true, out, elapsedMs = elapsed)
        } catch (t: Throwable) {
            ToolResult(false, emptyMap(), t.message ?: t::class.simpleName, (System.nanoTime() - started) / 1_000_000)
        }
    }
}
''',
'permission/AiPermissionPolicy.kt': r'''package com.waheed.artificerx.core.ai.apex.permission

enum class AiPermission { READ_CANVAS, WRITE_CANVAS, READ_FILE, WRITE_FILE, NETWORK, DELETE_PROJECT, RUN_CODE, EXPORT }
data class AiPolicy(val granted: Set<AiPermission>, val confirmationRequired: Set<AiPermission> = emptySet()) {
    fun can(permission: AiPermission, confirmed: Boolean = false): Boolean = permission in granted && (permission !in confirmationRequired || confirmed)
}
class AiPermissionPolicy(initial: AiPolicy = AiPolicy(emptySet())) {
    private var policy = initial
    @Synchronized fun update(next: AiPolicy) { policy = next }
    @Synchronized fun can(permission: AiPermission, confirmed: Boolean = false): Boolean = policy.can(permission, confirmed)
    @Synchronized fun snapshot(): AiPolicy = policy
}
''',
'generation/GenerationGateway.kt': r'''package com.waheed.artificerx.core.ai.apex.generation

enum class GenerationKind { IMAGE, IMAGE_TO_IMAGE, SKETCH_TO_IMAGE, VARIATION }
data class GenerationRequest(val kind: GenerationKind, val prompt: String, val sourceImage: IntArray? = null, val seed: Long? = null, val parameters: Map<String, String> = emptyMap())
data class GeneratedArtifact(val mime: String, val width: Int, val height: Int, val pixels: IntArray?, val providerId: String, val metadata: Map<String, String> = emptyMap())
interface GenerationAdapter { val id: String; fun supports(kind: GenerationKind): Boolean; fun generate(request: GenerationRequest): Result<GeneratedArtifact> }
class GenerationGateway(private val adapters: MutableList<GenerationAdapter> = mutableListOf()) {
    fun register(adapter: GenerationAdapter) { adapters.removeAll { it.id == adapter.id }; adapters += adapter }
    fun generate(request: GenerationRequest): Result<GeneratedArtifact> {
        require(request.prompt.isNotBlank())
        val capable = adapters.filter { it.supports(request.kind) }
        if (capable.isEmpty()) return Result.failure(IllegalStateException("No generation adapter registered for ${request.kind}"))
        var last: Result<GeneratedArtifact> = Result.failure(IllegalStateException("no_attempt"))
        for (adapter in capable) { last = adapter.generate(request); if (last.isSuccess) return last }
        return last
    }
}
''',
'generation/DeterministicGenerationAdapter.kt': r'''package com.waheed.artificerx.core.ai.apex.generation

/** Deterministic fallback adapter: creates a simple raster artifact so workflows can remain testable offline. */
class DeterministicGenerationAdapter : GenerationAdapter {
    override val id: String = "deterministic-offline"
    override fun supports(kind: GenerationKind): Boolean = kind in setOf(GenerationKind.IMAGE, GenerationKind.VARIATION)
    override fun generate(request: GenerationRequest): Result<GeneratedArtifact> = runCatching {
        val width = 256; val height = 256
        val pixels = IntArray(width * height)
        val seed = request.seed ?: request.prompt.hashCode().toLong()
        var x = seed xor 0x9E3779B97F4A7C15L
        for (i in pixels.indices) { x = x * -7046029254386353131L + 3037000493L; val v = (x ushr 32).toInt() and 0xff; pixels[i] = 0xff000000.toInt() or (v shl 16) or ((255 - v) shl 8) or v }
        GeneratedArtifact("image/raw-argb", width, height, pixels, id, mapOf("deterministic" to "true", "promptHash" to request.prompt.hashCode().toString()))
    }
}
''',
'generation/AdapterContracts.kt': r'''package com.waheed.artificerx.core.ai.apex.generation

enum class AdapterKind { LOCAL, REMOTE, IMAGE_GENERATION, VISION, EMBEDDING }
data class AdapterHealth(val id: String, val kind: AdapterKind, val healthy: Boolean, val latencyMs: Long, val message: String = "")
interface HealthAwareAdapter { val health: AdapterHealth }
''',
'workflow/AutonomousWorkflowEngine.kt': r'''package com.waheed.artificerx.core.ai.apex.workflow

import com.waheed.artificerx.core.ai.apex.agent.AgentTaskGraph

data class WorkflowState(val completed: Set<String> = emptySet(), val failed: Set<String> = emptySet(), val outputs: Map<String, String> = emptyMap())
data class WorkflowStepResult(val nodeId: String, val success: Boolean, val output: String = "", val error: String? = null)

class AutonomousWorkflowEngine {
    fun run(graph: AgentTaskGraph, initial: WorkflowState = WorkflowState(), handler: (String, String) -> WorkflowStepResult): WorkflowState {
        var state = initial
        while (true) {
            val ready = graph.ready(state.completed)
            if (ready.isEmpty()) break
            var progressed = false
            for (node in ready) {
                val result = handler(node.id, node.description)
                if (result.success) {
                    state = state.copy(completed = state.completed + node.id, outputs = state.outputs + (node.id to result.output))
                } else {
                    state = state.copy(failed = state.failed + node.id, outputs = state.outputs + (node.id to (result.error ?: "failed")))
                }
                progressed = true
            }
            if (!progressed) break
        }
        return state
    }
}
''',
'workflow/RetryingWorkflowExecutor.kt': r'''package com.waheed.artificerx.core.ai.apex.workflow

class RetryingWorkflowExecutor(private val maxAttempts: Int = 3, private val backoffMs: Long = 10L) {
    fun <T> run(block: () -> T): Result<T> {
        require(maxAttempts > 0)
        var last: Result<T> = Result.failure(IllegalStateException("not_run"))
        repeat(maxAttempts) { attempt ->
            last = runCatching(block)
            if (last.isSuccess) return last
            if (attempt + 1 < maxAttempts) Thread.sleep(backoffMs * (attempt + 1))
        }
        return last
    }
}
''',
'hybrid/HybridAiRuntime.kt': r'''package com.waheed.artificerx.core.ai.apex.hybrid

import com.waheed.artificerx.core.ai.apex.agent.ApexAgentKernel
import com.waheed.artificerx.core.ai.apex.generation.GenerationGateway
import com.waheed.artificerx.core.ai.apex.model.ModelMode
import com.waheed.artificerx.core.ai.apex.model.ModelRouter

data class HybridRequest(val prompt: String, val mode: ModelMode, val vision: Boolean, val tools: Boolean, val projectId: String, val canvasRevision: Long)
data class HybridDecision(val selectedModel: String?, val planNodes: Int, val generationAvailable: Boolean, val reasons: List<String>)
class HybridAiRuntime(private val router: ModelRouter, private val kernel: ApexAgentKernel, private val generation: GenerationGateway) {
    fun decide(request: HybridRequest): HybridDecision {
        val selection = router.select(request.mode, request.vision, request.tools)
        val graph = kernel.plan(request.prompt, request.projectId, request.canvasRevision)
        return HybridDecision(selection?.endpoint?.id, graph.nodes.size, generation != null, selection?.reasons ?: listOf("no_capable_model"))
    }
}
''',
'index/ApexCapabilityRegistry.kt': r'''package com.waheed.artificerx.core.ai.apex.index

import com.waheed.artificerx.core.ai.apex.tool.ToolRuntime
import com.waheed.artificerx.core.ai.apex.tool.ToolSpec

enum class ApexCapability { AGENT_OS, CANVAS_INTELLIGENCE, VISUAL_CRITIC, MULTI_AGENT, CREATIVE_MEMORY, GENERATION, AUTONOMOUS_WORKFLOW, HYBRID_RUNTIME }
class ApexCapabilityRegistry(private val tools: ToolRuntime) {
    private val capabilities = linkedSetOf<ApexCapability>()
    fun enable(capability: ApexCapability) { capabilities += capability }
    fun enabled(): Set<ApexCapability> = capabilities.toSet()
    fun registerCoreTools() {
        listOf(
            ToolSpec("canvas.inspect", 1, false, true),
            ToolSpec("canvas.propose", 1, false, true),
            ToolSpec("canvas.verify", 1, false, true),
            ToolSpec("vision.diff", 1, false, true),
            ToolSpec("memory.recall", 1, false, true),
            ToolSpec("memory.remember", 1, false, false),
            ToolSpec("generation.request", 1, false, false),
            ToolSpec("workflow.run", 1, false, false),
        ).forEach(tools::register)
    }
}
''',
'kernel/ApexRuntime.kt': r'''package com.waheed.artificerx.core.ai.apex.kernel

import com.waheed.artificerx.core.ai.apex.ContextEngine
import com.waheed.artificerx.core.ai.apex.generation.DeterministicGenerationAdapter
import com.waheed.artificerx.core.ai.apex.generation.GenerationGateway
import com.waheed.artificerx.core.ai.apex.index.ApexCapability
import com.waheed.artificerx.core.ai.apex.index.ApexCapabilityRegistry
import com.waheed.artificerx.core.ai.apex.memory.CreativeMemoryStore
import com.waheed.artificerx.core.ai.apex.model.ModelEndpoint
import com.waheed.artificerx.core.ai.apex.model.ModelRouter
import com.waheed.artificerx.core.ai.apex.tool.ToolRuntime
import com.waheed.artificerx.core.ai.apex.verify.VerificationEngine
import com.waheed.artificerx.core.ai.apex.vision.CanvasWorldModel
import com.waheed.artificerx.core.ai.apex.vision.VisualCritic

data class ApexRuntimeBundle(val kernel: ApexAgentKernel, val context: ContextEngine, val memory: CreativeMemoryStore, val router: ModelRouter, val tools: ToolRuntime, val generation: GenerationGateway, val capabilities: ApexCapabilityRegistry)
object ApexRuntimeFactory {
    fun create(): ApexRuntimeBundle {
        val memory = CreativeMemoryStore()
        val router = ModelRouter(mutableListOf(ModelEndpoint("deterministic-local", true, false, true, 8_192)))
        val tools = ToolRuntime()
        val generation = GenerationGateway().also { it.register(DeterministicGenerationAdapter()) }
        val world = CanvasWorldModel()
        val critic = VisualCritic()
        val verifier = VerificationEngine()
        val kernel = ApexAgentKernel(router, tools, memory, world, critic, verifier)
        val capabilities = ApexCapabilityRegistry(tools).also {
            it.registerCoreTools()
            ApexCapability.values().forEach(it::enable)
        }
        return ApexRuntimeBundle(kernel, ContextEngine(), memory, router, tools, generation, capabilities)
    }
}
''',
}
for rel, text in files.items():
    p = root / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text.strip()+"\n")
print(f"created {len(files)} apex files")
