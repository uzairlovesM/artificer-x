package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.network.ChatCompletionRequest
import com.waheed.artificerx.core.chat.ChatProfile
import com.waheed.artificerx.core.network.ChatCompletionResponse
import com.waheed.artificerx.core.network.ChatCompletionStreamChunkDto
import com.waheed.artificerx.core.network.ChatMessageDto
import com.waheed.artificerx.core.network.ContentPartDto
import com.waheed.artificerx.core.network.FunctionCallDto
import com.waheed.artificerx.core.network.ImageUrlDto
import com.waheed.artificerx.core.network.StreamToolCallDeltaDto
import com.waheed.artificerx.core.network.ToolCallDto
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import com.waheed.artificerx.data.workspace.MemoryRepository
import com.waheed.artificerx.core.routing.ModelRoutingPolicy
import com.waheed.artificerx.domain.model.AiProviderConfig
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 155's Agent Event Loop and Section 179/180's "no diffusion,
 * pure tool-calling vision-reasoning" architecture, made concrete.
 *
 * One handleUserMessage() call runs the full loop:
 *  1. Build the message list (system prompt + conversation history +
 *     the new user message, with an optional image_url content part
 *     for reference-image attachments).
 *  2. POST to the primary provider's /chat/completions with every tool
 *     from ToolRegistry attached.
 *  3. If the response contains tool_calls, execute each via
 *     ToolExecutor against the live StudioViewModel, append a
 *     "tool" role message with the result for every call, and loop
 *     back to step 2 — this is the vision-feedback/self-correction
 *     cycle (Section 156).
 *  4. Stop when the model calls finish_turn, or MAX_ITERATIONS is hit
 *     (a hard safety ceiling so a confused model can never loop
 *     forever burning free-tier quota — Section 191 Reliability).
 *  5. On any HTTP failure from the primary provider, fall back to the
 *     next enabled provider in priority order (primary first, then
 *     others by creation order) rather than failing the whole turn —
 *     Section 165's BackendAdapter multi-provider resilience.
 *
 * Emits AgentEvent as a Flow so AgentChatViewModel can render live
 * tool-call chips instead of waiting silently for a final answer.
 */
@Singleton
class AgentOrchestrator
    @Inject
    constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
        private val providerConfigRepository: ProviderConfigRepository,
        private val toolExecutor: ToolExecutor,
        private val snapshotEncoder: com.waheed.artificerx.core.render.CanvasSnapshotEncoder,
        private val agentSettingsDataStore: com.waheed.artificerx.data.local.datastore.AgentSettingsDataStore,
        private val agentPlanner: com.waheed.artificerx.core.agent.multiagent.AgentPlanner,
        private val worldModelStore: com.waheed.artificerx.core.agent.multiagent.WorldModelStore,
        private val localModelRepository: com.waheed.artificerx.data.repository.LocalModelRepository,
        private val localInferenceEngine: LocalInferenceEngine,
        private val htmlFetcher: com.waheed.artificerx.core.web.HtmlFetcher,
        private val webSearcher: com.waheed.artificerx.core.web.WebSearcher,
        private val memoryRepository: MemoryRepository,
        private val contextAssembler: AgentContextAssembler,
    ) {
        // Section 84/135/136/137: device-state-aware throttling. Cast
        // rather than a constructor-injected type, since ArtificerXApp
        // (@HiltAndroidApp) can't itself be a regular @Inject constructor
        // parameter without risking a circular part of Hilt's generated
        // component graph — @ApplicationContext Context is always safe to
        // inject and IS the Application instance at runtime, so this cast
        // is guaranteed to succeed. Previously this device-state data
        // (battery/network/power-save flows in ArtificerXApp,
        // DeviceRuntimeState.shouldThrottleHeavyWork derived from them in
        // MainActivity) was built and exposed via a CompositionLocal that
        // literally nothing ever read — the whole throttling concept
        // existed only in doc comments, never in actual gating logic.
        private val deviceStateApp: com.waheed.artificerx.ArtificerXApp
            get() = appContext as com.waheed.artificerx.ArtificerXApp

        private val localLlamaAdapter by lazy {
            com.waheed.artificerx.core.network
                .LocalLlamaAdapter(localInferenceEngine)
        }

        private val client =
            OkHttpClient
                .Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

        private val json =
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                explicitNulls = false
            }

        fun handleUserMessage(
            userText: String,
            attachedImageBase64: String?,
            conversationHistory: List<ChatMessageDto>,
            studioViewModel: StudioViewModel?,
            snapshotProvider: SnapshotProvider,
            projectId: String? = null,
            is3DMode: Boolean = false,
            chatProfile: ChatProfile? = null,
        ): Flow<AgentEvent> =
            flow {
                val needs = ModelRoutingPolicy.RequestNeeds(vision = attachedImageBase64 != null, toolCalling = true, offlineOnly = !deviceStateApp.isNetworkAvailable.value)
                var providers = collectUsableProviders(needs)
                if (chatProfile?.providerId != null) {
                    providers = providers.sortedByDescending { it.id == chatProfile.providerId }
                }
                if (chatProfile?.modelId != null) {
                    providers = providers.map { it.copy(defaultModelId = chatProfile.modelId) }
                }
                if (providers.isEmpty()) {
                    emit(AgentEvent.Error("No enabled AI provider is configured.", isFatal = true))
                    return@flow
                }

                // Section 84/135/136/137: device-state-aware throttling. Only
                // remote providers actually need network — a LOCAL_GGUF-only
                // setup should work exactly the same offline as it does
                // online, so this check is skipped entirely when every usable
                // provider is local. A metered connection is intentionally
                // NOT blocked here (only genuinely offline is) — throttling
                // an agent turn just because the user is on mobile data would
                // be presumptuous for an app with no data-usage estimate to
                // show them first.
                val hasOnlyLocalProviders = providers.all { it.type == com.waheed.artificerx.domain.model.AiProviderType.LOCAL_GGUF }
                if (!hasOnlyLocalProviders && !deviceStateApp.isNetworkAvailable.value) {
                    emit(
                        AgentEvent.Error(
                            "No network connection — the configured AI provider needs internet access. " +
                                "Import a local GGUF model in Settings → Local Model for fully offline use.",
                            isFatal = true,
                        ),
                    )
                    return@flow
                }

                // Non-blocking heads-up only — a low battery genuinely
                // shouldn't stop the user from working, but a multi-tool-call
                // agent turn on a local GGUF model (which is real, sustained
                // CPU load — see LocalInferenceEngine) is exactly the kind of
                // background work worth a one-time notice on, so the user can
                // decide whether to plug in first rather than discovering a
                // dead battery mid-turn.
                if (deviceStateApp.isBatteryLow.value) {
                    emit(
                        AgentEvent.Error(
                            "Battery is low — this turn will still run, but consider plugging in for longer sessions.",
                            isFatal = false,
                        ),
                    )
                }

                val agentSettings = collectAgentSettings()
                val executionBudget = AgentExecutionPolicy.budget(userText, agentSettings.effectiveMaxIterations, deviceStateApp.isNetworkAvailable.value)
                val effectiveMaxIterations = executionBudget.maxIterations
                val effectiveTemperature = chatProfile?.temperature ?: agentSettings.effectiveTemperature
                val artifactIntent = ArtifactIntentDetector.detect(userText)

                val selectedRole = agentPlanner.selectRole(userText, is3DMode)
                val worldModel = if (projectId != null) worldModelStore.get(projectId) else null

                val systemMessage = systemPromptMessage(selectedRole, worldModel, agentSettings, AgentIntentRouter.route(userText), artifactIntent)
                val compiledContext = AgentContextCompiler.compile(
                    system = systemMessage,
                    history = conversationHistory,
                    user = userTurnMessage(userText, attachedImageBase64),
                    // No application-side context truncation. Provider/model context remains
                    // authoritative; the compiler retains as much conversation as supplied.
                    maxCharacters = Int.MAX_VALUE,
                )
                val messages = compiledContext.messages.toMutableList()

                var providerIndex = 0
                var iteration = 0
                var finished = false
                var turnPhase = TurnPhase.MAIN
                var mainTurnSummary: String? = null
                var totalToolCalls = 0

                while (!finished && iteration < effectiveMaxIterations) {
                    iteration++
                    val provider = providers.getOrNull(providerIndex)
                    if (provider == null) {
                        emit(AgentEvent.Error("All configured providers failed or are exhausted.", isFatal = true))
                        return@flow
                    }

                    emit(AgentEvent.ThinkingStarted(provider.displayName))

                    // v0.4.30 REAL STREAMING: local GGUF still goes through
                    // the old one-shot callProvider (see its own doc note
                    // for why — the native llama.cpp bridge needs its own
                    // token-callback wiring, tracked as separate follow-up
                    // work and NOT silently pretended to be solved here).
                    // Every cloud provider (Groq/OpenRouter/Cloudflare/
                    // custom) now genuinely streams: streamCloudProvider
                    // emits a real AgentEvent.AgentTextChunk per SSE delta
                    // as it arrives over the socket, not one fake chunk
                    // containing the whole finished reply.
                    val turnResult: TurnCallResult? =
                        if (provider.type == com.waheed.artificerx.domain.model.AiProviderType.LOCAL_GGUF) {
                            val response = callProvider(provider, messages, userText, effectiveTemperature.toDouble())
                            val message = response?.choices?.firstOrNull()?.message
                            if (message?.contentText?.isNotBlank() == true) {
                                emit(AgentEvent.AgentTextChunk(message.contentText))
                            }
                            response?.let { TurnCallResult(message, it.choices.firstOrNull()?.finishReason) }
                        } else {
                            streamCloudProvider(provider, messages, userText, effectiveTemperature.toDouble(), agentSettings.reasoningEffort) { event -> emit(event) }
                        }

                    if (turnResult == null || turnResult.message == null) {
                        if (providerIndex + 1 < providers.size) {
                            emit(
                                AgentEvent.ProviderFallback(
                                    fromProvider = provider.displayName,
                                    toProvider = providers[providerIndex + 1].displayName,
                                    reason = "Request failed or timed out",
                                ),
                            )
                            providerIndex++
                            continue
                        } else {
                            emit(AgentEvent.Error("Every configured provider failed to respond.", isFatal = true))
                            return@flow
                        }
                    }

                    providerConfigRepository.incrementUsage(provider.toRecordShapeForUsage())

                    val assistantMessage = turnResult.message
                    messages.add(assistantMessage)

                    val toolCalls = assistantMessage.toolCalls
                    if (toolCalls.isNullOrEmpty()) {
                        if (LongOutputContinuationPlanner.shouldContinue(turnResult.finishReason)) {
                            messages.add(
                                ChatMessageDto(
                                    role = "user",
                                    contentText = LongOutputContinuationPlanner.prompt(assistantMessage.contentText ?: ""),
                                ),
                            )
                            emit(AgentEvent.AgentTextChunk("\n\n[Continuing because the provider reached its output boundary…]\n\n"))
                            continue
                        }
                        finished = true
                        continue
                    }

                    var snapshotRequestedThisRound = false

                    for (toolCall in toolCalls) {
                        totalToolCalls++
                        if (totalToolCalls == Int.MAX_VALUE) {
                            emit(AgentEvent.Error("Application tool-call counter exhausted. Start a new turn to continue.", isFatal = true))
                            return@flow
                        }
                        emit(
                            AgentEvent.ToolCallStarted(
                                callId = toolCall.id,
                                toolName = toolCall.function.name,
                                argsPreview = toolCall.function.arguments.take(120),
                            ),
                        )

                        val parsed = ToolCallParser.parse(toolCall)

                        if (turnPhase == TurnPhase.CRITIC &&
                            parsed !is ParsedToolCall.FinishTurn &&
                            parsed !is ParsedToolCall.InspectCanvas &&
                            parsed !is ParsedToolCall.InspectScene
                        ) {
                            emit(
                                AgentEvent.ToolCallFailed(
                                    toolCall.id,
                                    "Critic phase is read-only — call finish_turn with your verdict instead.",
                                ),
                            )
                            messages.add(
                                toolResultMessage(
                                    toolCall,
                                    "ERROR: Critic phase cannot modify the canvas or scene. Call finish_turn with APPROVED: or NEEDS_REPAIR: instead.",
                                ),
                            )
                            continue
                        }

                        val result =
                            if (parsed is ParsedToolCall.WebFetch) {
                                // Section: Web search/fetch tools — a real network
                                // call, handled here rather than inside the
                                // synchronous ToolExecutor.execute()/executeSculptOnly()
                                // dispatch (which runs on Dispatchers.Main.immediate
                                // for canvas-thread safety and was never meant to
                                // block on I/O).
                                executeWebFetch(parsed)
                            } else if (parsed is ParsedToolCall.WebSearch) {
                                executeWebSearch(parsed)
                            } else {
                                withContext(Dispatchers.Main.immediate) {
                                    if (studioViewModel != null) {
                                        toolExecutor.execute(parsed, studioViewModel)
                                    } else {
                                        toolExecutor.executeSculptOnly(parsed)
                                    }
                                }
                            }

                        when (result) {
                            is ToolExecutionResult.Success -> {
                                emit(AgentEvent.ToolCallSucceeded(toolCall.id, result.message))
                                messages.add(toolResultMessage(toolCall, result.message))
                                if (result.requiresSnapshot) snapshotRequestedThisRound = true
                            }
                            is ToolExecutionResult.Failure -> {
                                val repair = AgentRepairPlanner.classify(result.errorMessage)
                                emit(AgentEvent.ToolCallFailed(toolCall.id, result.errorMessage))
                                messages.add(toolResultMessage(toolCall, "ERROR: ${result.errorMessage}\nREPAIR GUIDANCE: ${repair.guidance}"))
                            }
                            is ToolExecutionResult.TurnFinished -> {
                                emit(AgentEvent.ToolCallSucceeded(toolCall.id, "Turn finished"))
                                messages.add(toolResultMessage(toolCall, "Turn finished: ${result.summary}"))

                                when (turnPhase) {
                                    TurnPhase.CRITIC -> {
                                        val verdict = result.summary
                                        val approved = verdict.trim().startsWith("APPROVED", ignoreCase = true)
                                        emit(AgentEvent.CriticReview(approved = approved, detail = verdict))
                                        if (approved || !agentSettings.enableCriticPass) {
                                            emit(AgentEvent.TurnCompleted(mainTurnSummary ?: verdict))
                                            finished = true
                                        } else {
                                            emit(AgentEvent.RepairStarted(verdict))
                                            turnPhase = TurnPhase.REPAIR
                                            messages.add(
                                                systemPromptMessage(com.waheed.artificerx.core.agent.multiagent.AgentRole.REPAIR, null),
                                            )
                                            messages.add(
                                                ChatMessageDto(
                                                    role = "user",
                                                    contentText = "Critic's finding to address: $verdict",
                                                ),
                                            )
                                        }
                                    }
                                    TurnPhase.REPAIR -> {
                                        emit(AgentEvent.TurnCompleted(result.summary))
                                        finished = true
                                    }
                                    TurnPhase.MAIN -> {
                                        mainTurnSummary = result.summary
                                        val canRunCritic =
                                            projectId != null &&
                                                agentSettings.enableCriticPass &&
                                                selectedRole !in com.waheed.artificerx.core.agent.multiagent.AgentRole.READ_ONLY_ROLES
                                        if (canRunCritic) {
                                            turnPhase = TurnPhase.CRITIC
                                            messages.add(
                                                systemPromptMessage(
                                                    com.waheed.artificerx.core.agent.multiagent.AgentRole.CRITIC,
                                                    worldModel,
                                                ),
                                            )
                                            messages.add(
                                                ChatMessageDto(
                                                    role = "user",
                                                    contentText = "Review the work you just completed against the original request.",
                                                ),
                                            )
                                        } else {
                                            emit(AgentEvent.TurnCompleted(result.summary))
                                            finished = true
                                        }
                                        if (projectId != null &&
                                            selectedRole !in com.waheed.artificerx.core.agent.multiagent.AgentRole.READ_ONLY_ROLES
                                        ) {
                                            runArchivistPass(projectId, providers.getOrNull(providerIndex), messages)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (snapshotRequestedThisRound && !finished) {
                        val snapshotBitmap =
                            withContext(Dispatchers.Main.immediate) {
                                snapshotProvider.captureSnapshot()
                            }
                        if (snapshotBitmap != null) {
                            val snapshotBase64 = snapshotEncoder.encodeForVisionFeedback(snapshotBitmap)
                            messages.add(visionFeedbackMessage(snapshotBase64))
                        }
                    }
                }

                if (!finished && iteration >= effectiveMaxIterations) {
                    emit(AgentEvent.MaxIterationsReached)
                }
            }.flowOn(Dispatchers.IO)

        private suspend fun collectAgentSettings(): com.waheed.artificerx.data.local.datastore.AgentSettings =
            agentSettingsDataStore.settings.first()

        /** Providers whose known daily quota has been reached are pushed to
         *  the back rather than dropped entirely — a stale/incorrect quota
         *  number should degrade to "try it last", never to "never try it",
         *  since the provider's actual API response is the real source of
         *  truth on whether a call succeeds. */
        private suspend fun collectUsableProviders(needs: ModelRoutingPolicy.RequestNeeds): List<AiProviderConfig> {
            val enabled = providerConfigRepository.configs.first().filter { it.isEnabled }
            return ModelRoutingPolicy.rank(enabled, needs)
        }

        private suspend fun systemPromptMessage(
            role: com.waheed.artificerx.core.agent.multiagent.AgentRole,
            worldModel: com.waheed.artificerx.core.agent.multiagent.WorldModel?,
            agentSettings: com.waheed.artificerx.data.local.datastore.AgentSettings? = null,
            intentRoute: AgentIntentRouter.Route? = null,
            artifactIntent: ArtifactIntentDetector.Intent? = null,
        ): ChatMessageDto {
            val basePrompt =
                """
                You are the Reasoning Brain of ARTIFICER-X, an agentic art studio.

Interaction policy: Prefer doing over describing; inspect state before multi-step edits; make reversible changes; verify every side effect; never claim an artifact exists until a concrete tool succeeds.
                You do not generate images directly. You create artwork and 3D
                sculptures exclusively by calling the provided tools.

                CRITICAL CREATIVE QUALITY RULE: For scene requests, never draw a
                vague blob or a single arbitrary object. First infer the subject,
                camera/view, composition, spatial relationships, perspective,
                lighting, palette, material cues, focal hierarchy and intended
                style. For recognizable scenes such as anime rooms, studios,
                streets, bedrooms, kitchens or classrooms, use compose_scene to
                construct a structured multi-layer scene with architectural
                planes, perspective guides, major furniture silhouettes,
                foreground/midground/background separation, line art and light.
                Then inspect_canvas and repair weak geometry. A successful turn
                should visibly resemble the requested subject, not merely satisfy
                the existence of pixels.

                Artifact/workspace tools are real side-effecting operations: canvas drawing creates real raster output and publishes it through the artifact pipeline; create_file writes a real local artifact; create_zip packages real files; read_workspace_file/list_workspace_directory/write_workspace_file/replace_workspace_text operate on the managed works workspace; remember and recall persist local memory; run_terminal_command and run_terminal_batch execute in the app-private sandbox. Never claim an artifact, image, ZIP, terminal result, or memory entry exists unless its tool returned success.

                Prefer concrete named tools over dynamic capability aliases. Dynamic tools are routed only through supported local action adapters; unsupported dynamic actions must be reported as unsupported rather than invented.

                Before coding/build/environment work, call inspect_android_toolchain so your implementation choices match the real private device/build environment.

                2D canvas tools: create_layer, delete_layer, set_active_layer,
                draw_path, draw_shape, apply_gradient, fill_region,
                set_layer_property, pick_color, apply_filter, add_text,
                create_mask, enable_symmetry, apply_pattern, draw_curve,
                import_image_layer, compose_scene, inspect_canvas, set_selection,
                clear_selection, delete_selection_content, transform_layer,
                web_search, web_fetch, resize_canvas, set_canvas_background,
                set_brush_defaults.

                Note: clear_selection only deselects (drops the rectangle,
                touches no pixels) — use delete_selection_content when you
                actually want to erase what's inside a selection.

                resize_canvas / set_canvas_background / set_brush_defaults give
                you full control over the project itself, not just what you draw
                inside it — call resize_canvas FIRST whenever a request implies a
                specific format (poster dimensions, square post, wallpaper aspect
                ratio) before creating layers. Use set_brush_defaults once to lock
                in a brush type/size/color for a whole sequence of draw_path calls
                instead of repeating those fields on every single call.

                3D sculpting tools: create_primitive, sculpt_stroke, delete_mesh,
                set_mesh_color, transform_mesh, inspect_scene.

                For coding work, use read_workspace_file and list_workspace_directory before editing. Use replace_workspace_text for surgical patches or write_workspace_file for complete files, then inspect the resulting state and run available checks. Keep all edits inside the managed ARTIFICER-X/works workspace unless a concrete tool explicitly targets another safe location.

                Work in small deliberate steps. After any operation whose result
                you are not certain about, call inspect_canvas (2D) or
                inspect_scene (3D) to see the current state before continuing.
                When the requested work is complete, call finish_turn with a
                short summary. Never describe what you would create in plain
                text instead of calling a tool — plain text alone produces
                nothing on the canvas or in the scene.
                """.trimIndent()

            // v0.4.30 Deep Studio mode: this is the actual difference
            // between "AI draws one flat sloppy thing" and "AI researches,
            // plans, and builds like a real illustrator" — a concrete
            // mandatory workflow, not just "try harder" framing. Every
            // step below maps to real tool calls the model already has
            // (web_search/web_fetch are genuinely wired, not decorative —
            // see webSearchTool/webFetchTool in ToolExecutor).
            val deepStudioSection =
                if (agentSettings?.isDeepStudioMode == true) {
                    """


                    DEEP STUDIO MODE IS ACTIVE for this turn. You have a large
                    tool-call budget (${agentSettings.effectiveMaxIterations} calls) — use it. A
                    rushed, single-layer result is a FAILURE in this mode even if it
                    technically completes. Follow this workflow for any creative
                    request (character, scene, background, object, pattern):

                    1. RESEARCH FIRST. Call web_search for real visual/anatomical/
                       stylistic reference before drawing anything (e.g. "anime eye
                       anatomy front view proportions", "cel shading technique
                       layer order", "[subject] color palette reference"). Call
                       web_fetch on the most relevant result to read real detail,
                       not just the search snippet. Do this for every distinct
                       element you are unsure how to construct correctly (eyes,
                       hands, hair flow, folds, perspective, lighting direction).
                    2. PLAN explicitly in your text response before drawing: list
                       the layers you will build and in what order, and what
                       reference informed each one.
                    3. BUILD IN REAL SEPARATE LAYERS via create_layer — never
                       flatten a whole character/scene into one or two layers.
                       A typical build order: rough sketch layer -> base color
                       (flat fills) layer -> shading/shadow layer -> highlights
                       layer -> line art / detail layer -> background layer ->
                       effects/atmosphere layer. Name each layer descriptively
                       (set_layer_property) so the layer list itself documents
                       the construction.
                    4. SELF-CHECK with inspect_canvas between major phases and
                       compare against what you researched — fix proportions or
                       color choices that don't match reference before moving on.
                    5. Only call finish_turn once every planned layer exists and
                       you've verified the result against your own plan.

                    Do not skip the research step because you feel confident —
                    confidence without a real web_search/web_fetch call in this
                    mode is exactly the "ghatiya" low-effort output this mode
                    exists to eliminate.
                    """.trimIndent()
                } else {
                    ""
                }

            val roleSection = "\n\nCurrent specialist focus (${role.displayName}):\n${role.focusInstruction}"
            val worldModelSection = worldModel?.toPromptBlock()?.let { "\n\n$it" } ?: ""
            val memorySection = runCatching { memoryRepository.list("global").take(12) }.getOrDefault(emptyList()).let { memories ->
                if (memories.isEmpty()) "" else "\n\nPersistent local memory:\n" + memories.joinToString("\n") { "- ${it.key}: ${it.value.take(400)}" }
            }
            val routeSection = intentRoute?.let { route ->
                "\n\nIntent route: ${route.kind.name} (${route.confidence}% confidence). Preferred tools: ${route.preferredTools.joinToString(", ")}. Guidance: ${route.notes}"
            } ?: ""
            val artifactIntentSection = artifactIntent?.takeIf { it.explicit }?.let { intent ->
                "\n\nExplicit output intent: ${intent.requested.joinToString(", ")}. Materialize the requested output instead of returning a text-only description."
            } ?: ""

            return ChatMessageDto(
                role = "system",
                contentText = basePrompt + deepStudioSection + roleSection + worldModelSection + memorySection + routeSection + artifactIntentSection,
            )
        }

        /** Section "Archivist" role: a lightweight follow-up call that reads
         *  what just happened and extracts an updated World Model summary,
         *  so the next turn — whether handled by the same role or a
         *  different specialist — has continuity. Runs after the user's
         *  turn has already completed (TurnCompleted was already emitted)
         *  so a slow or failed Archivist call never delays or breaks the
         *  turn the user is actually waiting on; failures are swallowed. */
        private suspend fun runArchivistPass(
            projectId: String,
            provider: AiProviderConfig?,
            conversationSoFar: List<ChatMessageDto>,
        ) {
            if (provider == null) return
            runCatching {
                val archivistMessages = mutableListOf<ChatMessageDto>()
                archivistMessages.add(systemPromptMessage(com.waheed.artificerx.core.agent.multiagent.AgentRole.ARCHIVIST, null))
                archivistMessages.addAll(conversationSoFar.takeLast(ARCHIVIST_CONTEXT_MESSAGE_COUNT))

                val response = callProvider(provider, archivistMessages, "archivist continuity review", temperature = 0.2)
                val summaryText =
                    response
                        ?.choices
                        ?.firstOrNull()
                        ?.message
                        ?.let { it.contentText ?: it.contentParts?.firstOrNull { part -> part.type == "text" }?.text }
                        ?: return

                if (summaryText.isNotBlank()) {
                    worldModelStore.mergeUpdate(projectId = projectId, additionalNotes = summaryText.take(MAX_ARCHIVIST_NOTE_LENGTH))
                }
            }
        }

        private companion object {
            const val ARCHIVIST_CONTEXT_MESSAGE_COUNT = 6
            const val MAX_ARCHIVIST_NOTE_LENGTH = 800
        }

        /** Tracks which stage of the optional Critic/Repair loop (Section
         *  "Critic/Repair") the current handleUserMessage() call is in.
         *  MAIN is the normal work phase; a MAIN finish_turn call moves to
         *  CRITIC only when AgentSettings.enableCriticPass is on; CRITIC's
         *  own finish_turn either ends the turn (approved) or moves to
         *  REPAIR; REPAIR's finish_turn always ends the turn — repair
         *  results are never re-reviewed, to guarantee termination rather
         *  than risking an approve/reject ping-pong. */
        private enum class TurnPhase { MAIN, CRITIC, REPAIR }

        private fun userTurnMessage(
            text: String,
            attachedImageBase64: String?,
        ): ChatMessageDto {
            if (attachedImageBase64 == null) {
                return ChatMessageDto(role = "user", contentText = text)
            }
            val parts =
                listOf(
                    ContentPartDto(type = "text", text = text),
                    ContentPartDto(type = "image_url", imageUrl = ImageUrlDto(url = "data:image/png;base64,$attachedImageBase64")),
                )
            return ChatMessageDto(role = "user", contentParts = parts)
        }

        private fun toolResultMessage(
            toolCall: ToolCallDto,
            resultText: String,
        ): ChatMessageDto =
            ChatMessageDto(
                role = "tool",
                toolCallId = toolCall.id,
                name = toolCall.function.name,
                contentText = resultText,
            )

        /** Section 156 vision-feedback loop: injects the current canvas
         *  render as a user-role image message so the model can visually
         *  verify the effect of its last tool call(s) before deciding what
         *  to do next — this is what makes self-correction (Section 36)
         *  possible without a diffusion model in the loop. */
        private fun visionFeedbackMessage(base64Png: String): ChatMessageDto =
            ChatMessageDto(
                role = "user",
                contentParts =
                    listOf(
                        ContentPartDto(type = "text", text = "Here is the current canvas after your last action:"),
                        ContentPartDto(type = "image_url", imageUrl = ImageUrlDto(url = "data:image/png;base64,$base64Png")),
                    ),
            )

        /** Section: Web search/fetch tools. Runs the actual HTTP fetch +
         *  readable-content extraction (HtmlFetcher) and shapes the
         *  outcome into the same ToolExecutionResult vocabulary every
         *  other tool call produces, so the calling loop in
         *  handleUserMessage() doesn't need a separate success/failure
         *  path for this one tool. */
        private suspend fun executeWebFetch(call: ParsedToolCall.WebFetch): ToolExecutionResult {
            if (call.url.isBlank()) {
                return ToolExecutionResult.Failure("web_fetch requires a non-empty url argument.")
            }
            return when (val result = htmlFetcher.fetch(call.url)) {
                is com.waheed.artificerx.core.web.WebFetchResult.Success -> {
                    val titleLine = result.title?.let { "Title: $it\n" } ?: ""
                    val bylineLine = result.byline?.let { "By: $it\n" } ?: ""
                    ToolExecutionResult.Success(
                        message = "$titleLine$bylineLine\n${result.readableText}",
                        requiresSnapshot = false,
                    )
                }
                is com.waheed.artificerx.core.web.WebFetchResult.HttpError ->
                    ToolExecutionResult.Failure("Fetching ${result.url} failed: HTTP ${result.statusCode} ${result.message}")
                is com.waheed.artificerx.core.web.WebFetchResult.NetworkError ->
                    ToolExecutionResult.Failure("Fetching ${result.url} failed: ${result.message}")
                is com.waheed.artificerx.core.web.WebFetchResult.ExtractionFailed ->
                    ToolExecutionResult.Failure("Fetched ${result.url} but could not extract readable content: ${result.message}")
                is com.waheed.artificerx.core.web.WebFetchResult.Blocked ->
                    ToolExecutionResult.Failure("Refused to fetch ${result.url}: ${result.reason}")
            }
        }

        /** v0.4.30: the real research call behind Deep Studio mode's
         *  mandatory "search before you draw" workflow — see
         *  WebSearcher's doc for the no-API-key DuckDuckGo approach.
         *  Same interception pattern as executeWebFetch above (real
         *  network I/O, kept off the Main-dispatched ToolExecutor). */
        private suspend fun executeWebSearch(call: ParsedToolCall.WebSearch): ToolExecutionResult {
            if (call.query.isBlank()) {
                return ToolExecutionResult.Failure("web_search requires a non-empty query argument.")
            }
            return when (val result = webSearcher.search(call.query)) {
                is com.waheed.artificerx.core.web.WebSearchResult.Success -> {
                    val formatted =
                        result.results.withIndex().joinToString("\n\n") { (index, item) ->
                            "${index + 1}. ${item.title}\n${item.url}\n${item.snippet}"
                        }
                    ToolExecutionResult.Success(
                        message = "Search results for \"${result.query}\":\n\n$formatted",
                        requiresSnapshot = false,
                    )
                }
                is com.waheed.artificerx.core.web.WebSearchResult.NoResults ->
                    ToolExecutionResult.Success(
                        message = "No search results found for \"${result.query}\". Try a different or broader query.",
                        requiresSnapshot = false,
                    )
                is com.waheed.artificerx.core.web.WebSearchResult.NetworkError ->
                    ToolExecutionResult.Failure("Search for \"${result.query}\" failed: ${result.message}")
            }
        }

        /** v0.4.30 REAL STREAMING: result shape the main turn loop needs
         *  regardless of whether it came from the streamed cloud path or
         *  the one-shot local-model path — keeps the loop body identical
         *  either way. */
        private data class TurnCallResult(
            val message: ChatMessageDto?,
            val finishReason: String?,
        )

        private class ToolCallAccumulator {
            var id: String = ""
            var type: String = "function"
            var name: String = ""
            val argumentsBuilder = StringBuilder()
        }

        private sealed class StreamEvent {
            data class TextDelta(
                val text: String,
            ) : StreamEvent()

            data class ToolCallChunk(
                val delta: StreamToolCallDeltaDto,
            ) : StreamEvent()

            data class Finished(
                val reason: String?,
            ) : StreamEvent()

            data class Failed(
                val reason: String,
            ) : StreamEvent()
        }

        /** The actual fix: opens the HTTP request with stream = true, reads
         *  the response body's SSE lines one at a time off the socket as
         *  they arrive (not after the whole body is buffered — that's
         *  what `stream = false` + `response.body.string()` in the old
         *  [callProvider] did, which is exactly why the UI could never
         *  show real progressive text), and turns each `data: {...}` line
         *  into a [StreamEvent] the instant it's parsed. Text deltas are
         *  forwarded to the caller's [emit] immediately — that's the live
         *  token to the chat bubble. Tool-call deltas are accumulated
         *  (providers stream a tool call's name/arguments in fragments
         *  across many chunks, indexed by call slot) until the stream's
         *  finish_reason arrives, at which point a complete ChatMessageDto
         *  is assembled so the rest of the turn loop — tool execution,
         *  message history — works exactly as it did with the old
         *  non-streamed response.
         *
         *  Uses callbackFlow rather than a plain suspend function doing
         *  `withContext(Dispatchers.IO) { ...emit... }` deliberately: Kotlin
         *  Flow forbids calling emit() from a different coroutine context
         *  than the one collecting it ("flow invariant violated"), and the
         *  blocking OkHttp socket read has to run on Dispatchers.IO. Doing
         *  the read inside callbackFlow's own IO-dispatched child
         *  coroutine and forwarding through trySend(), then collecting
         *  that flow with a plain `.collect { }` back on the caller's own
         *  coroutine (where calling the passed-in `emit` lambda is legal),
         *  is the correct/safe bridge between "blocking network read" and
         *  "cooperative Flow emission" for exactly this situation. */
        private suspend fun streamCloudProvider(
            provider: AiProviderConfig,
            messages: List<ChatMessageDto>,
            userText: String,
            temperature: Double,
            reasoningEffort: String?,
            emit: suspend (AgentEvent) -> Unit,
        ): TurnCallResult? {
            val rawKey = providerConfigRepository.rawKeyFor(provider.keyAlias) ?: return null
            val modelId = provider.defaultModelId ?: defaultModelFor(provider)

            val requestBody =
                ChatCompletionRequest(
                    model = modelId,
                    messages = messages,
                    tools = ToolSelectionPolicy.select(userText),
                    temperature = temperature,
                    maxTokens = null,
                    stream = true,
                    reasoningEffort = reasoningEffort,
                )
            val bodyJson = json.encodeToString(ChatCompletionRequest.serializer(), requestBody)
            val request =
                Request
                    .Builder()
                    .url("${provider.baseUrl.trimEnd('/')}/chat/completions")
                    .header("Authorization", "Bearer $rawKey")
                    .header("Accept", "text/event-stream")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .build()

            val events: Flow<StreamEvent> =
                callbackFlow {
                    val call = client.newCall(request)
                    launch(Dispatchers.IO) {
                        try {
                            call.execute().use { response ->
                                if (!response.isSuccessful) {
                                    trySend(StreamEvent.Failed("HTTP ${response.code}"))
                                    return@use
                                }
                                val source = response.body?.source()
                                if (source == null) {
                                    trySend(StreamEvent.Failed("Empty response body"))
                                    return@use
                                }
                                while (!source.exhausted()) {
                                    val line = runCatching { source.readUtf8Line() }.getOrNull() ?: break
                                    if (line.isBlank() || !line.startsWith("data:")) continue
                                    val payload = line.removePrefix("data:").trim()
                                    if (payload == "[DONE]") break
                                    val chunk =
                                        runCatching {
                                            json.decodeFromString(ChatCompletionStreamChunkDto.serializer(), payload)
                                        }.getOrNull() ?: continue
                                    val choice = chunk.choices.firstOrNull() ?: continue
                                    val content = choice.delta.content
                                    if (!content.isNullOrEmpty()) {
                                        trySend(StreamEvent.TextDelta(content))
                                    }
                                    choice.delta.toolCalls?.forEach { toolDelta ->
                                        trySend(StreamEvent.ToolCallChunk(toolDelta))
                                    }
                                    if (choice.finishReason != null) {
                                        trySend(StreamEvent.Finished(choice.finishReason))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            trySend(StreamEvent.Failed(e.message ?: "Stream read error"))
                        } finally {
                            close()
                        }
                    }
                    awaitClose { call.cancel() }
                }

            val textBuilder = StringBuilder()
            val toolAccumulators = sortedMapOf<Int, ToolCallAccumulator>()
            var finishReason: String? = null
            var hadHardFailure = false

            events.collect { streamEvent ->
                when (streamEvent) {
                    is StreamEvent.TextDelta -> {
                        textBuilder.append(streamEvent.text)
                        emit(AgentEvent.AgentTextChunk(streamEvent.text))
                    }
                    is StreamEvent.ToolCallChunk -> {
                        val acc = toolAccumulators.getOrPut(streamEvent.delta.index) { ToolCallAccumulator() }
                        streamEvent.delta.id?.let { acc.id = it }
                        streamEvent.delta.type?.let { acc.type = it }
                        streamEvent.delta.function?.name?.let { acc.name = it }
                        streamEvent.delta.function?.arguments?.let { acc.argumentsBuilder.append(it) }
                    }
                    is StreamEvent.Finished -> finishReason = streamEvent.reason
                    is StreamEvent.Failed -> hadHardFailure = true
                }
            }

            if (hadHardFailure && textBuilder.isEmpty() && toolAccumulators.isEmpty()) return null

            val toolCallDtos =
                toolAccumulators.entries
                    .sortedBy { it.key }
                    .mapNotNull { (index, acc) ->
                        if (acc.name.isBlank()) return@mapNotNull null
                        ToolCallDto(
                            id = acc.id.ifBlank { "call_${provider.id}_${index}_${System.nanoTime()}" },
                            type = acc.type,
                            function = FunctionCallDto(name = acc.name, arguments = acc.argumentsBuilder.toString().ifBlank { "{}" }),
                        )
                    }

            val assistantMessage =
                ChatMessageDto(
                    role = "assistant",
                    contentText = if (toolCallDtos.isEmpty()) textBuilder.toString() else null,
                    toolCalls = toolCallDtos.ifEmpty { null },
                )
            return TurnCallResult(assistantMessage, finishReason)
        }

        private suspend fun callProvider(
            provider: AiProviderConfig,
            messages: List<ChatMessageDto>,
            userText: String,
            temperature: Double = 0.4,
        ): ChatCompletionResponse? {
            // Section: Local Model provider — bypass the OpenAI-compatible
            // HTTP path entirely for a local GGUF provider. There's no
            // baseUrl/API key to speak of; the active LocalModelInfo comes
            // from LocalModelRepository instead of AiProviderConfig's
            // network fields.
            if (provider.type == com.waheed.artificerx.domain.model.AiProviderType.LOCAL_GGUF) {
                val activeModel = localModelRepository.activeModel() ?: return null
                val response = localLlamaAdapter.generate(activeModel, messages, temperature)
                if (response != null) {
                    localModelRepository.touchLastUsed(activeModel.id)
                }
                return response
            }

            val rawKey = providerConfigRepository.rawKeyFor(provider.keyAlias) ?: return null
            val modelId = provider.defaultModelId ?: defaultModelFor(provider)

            val requestBody =
                ChatCompletionRequest(
                    model = modelId,
                    messages = messages,
                    tools = ToolSelectionPolicy.select(userText),
                    temperature = temperature,
                    maxTokens = null,
                )

            val bodyJson = json.encodeToString(ChatCompletionRequest.serializer(), requestBody)
            val request =
                Request
                    .Builder()
                    .url("${provider.baseUrl.trimEnd('/')}/chat/completions")
                    .header("Authorization", "Bearer $rawKey")
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .build()

            val call = client.newCall(request)
            return runCatching {
                // Wraps OkHttp's blocking Call.execute() so it actually
                // participates in coroutine cancellation: execute() itself
                // never suspends, so cancelling the surrounding coroutine
                // (e.g. AgentChatViewModel.stopCurrentTurn()) would
                // otherwise leave this exact HTTP request running to
                // completion on its OkHttp dispatcher thread regardless —
                // suspendCancellableCoroutine's invokeOnCancellation hook
                // is what actually calls call.cancel() and tears down the
                // underlying socket the moment the user taps Stop.
                kotlinx.coroutines.suspendCancellableCoroutine<ChatCompletionResponse?> { continuation ->
                    continuation.invokeOnCancellation { call.cancel() }
                    call.enqueue(
                        object : okhttp3.Callback {
                            override fun onResponse(
                                call: okhttp3.Call,
                                response: okhttp3.Response,
                            ) {
                                val result =
                                    response.use { resp ->
                                        if (!resp.isSuccessful) return@use null
                                        val responseBody = resp.body?.string() ?: return@use null
                                        runCatching {
                                            json.decodeFromString(ChatCompletionResponse.serializer(), responseBody)
                                        }.getOrNull()
                                    }
                                if (continuation.isActive) continuation.resume(result, onCancellation = null)
                            }

                            override fun onFailure(
                                call: okhttp3.Call,
                                e: java.io.IOException,
                            ) {
                                if (continuation.isActive) continuation.resume(null, onCancellation = null)
                            }
                        },
                    )
                }
            }.getOrNull()
        }

        /** CRITICAL FIX (v0.4.30): both hardcoded defaults below were dead
         *  model IDs — Groq decommissioned llama-3.2-90b-vision-preview and
         *  OpenRouter no longer serves a free Llama-3.2 vision variant. Every
         *  agent turn on a freshly-added provider (no defaultModelId saved
         *  yet) was silently calling a 404'd model, which either fails the
         *  whole turn or falls back with a useless response — this is the
         *  root cause behind "AI kuch nahi deta / dikhawa karta hai" for
         *  users who hadn't manually picked a model in Settings. Verified
         *  live via web search on 2026-09-02:
         *  - Groq's current vision+tool-calling model is qwen/qwen3.6-27b
         *    (multimodal, tool use, JSON mode).
         *  - OpenRouter's free Llama vision tier is gone; openrouter/free
         *    is their auto-router that filters for vision+tools+structured
         *    output automatically, so it survives free-model rotation
         *    instead of hardcoding an ID that dies in a few months again.
         */
        private fun defaultModelFor(provider: AiProviderConfig): String =
            when {
                provider.baseUrl.contains("groq", ignoreCase = true) -> "qwen/qwen3.6-27b"
                provider.baseUrl.contains("openrouter", ignoreCase = true) -> "openrouter/free"
                provider.baseUrl.contains("cloudflare", ignoreCase = true) -> "@cf/meta/llama-3.2-11b-vision-instruct"
                else -> "gpt-4o-mini"
            }

        private fun AiProviderConfig.toRecordShapeForUsage() =
            com.waheed.artificerx.data.local.datastore.ProviderConfigRecord(
                id = id,
                type = type.name,
                displayName = displayName,
                baseUrl = baseUrl,
                keyAlias = keyAlias,
                maskedKeyPreview = maskedKeyPreview,
                isEnabled = isEnabled,
                isPrimary = isPrimary,
                supportsVision = supportsVision,
                supportsToolCalling = supportsToolCalling,
                defaultModelId = defaultModelId,
                usageTodayCallCount = usageTodayCallCount,
                lastResetEpochDay = lastResetEpochDay,
                knownDailyQuota = knownDailyQuota,
                createdAtEpochMillis = createdAtEpochMillis,
            )
    }
