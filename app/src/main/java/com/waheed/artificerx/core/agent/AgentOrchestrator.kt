package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.network.ChatCompletionRequest
import com.waheed.artificerx.core.network.ChatCompletionResponse
import com.waheed.artificerx.core.network.ChatMessageDto
import com.waheed.artificerx.core.network.ContentPartDto
import com.waheed.artificerx.core.network.ImageUrlDto
import com.waheed.artificerx.core.network.ToolCallDto
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import com.waheed.artificerx.domain.model.AiProviderConfig
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
            }

        fun handleUserMessage(
            userText: String,
            attachedImageBase64: String?,
            conversationHistory: List<ChatMessageDto>,
            studioViewModel: StudioViewModel?,
            snapshotProvider: SnapshotProvider,
            projectId: String? = null,
            is3DMode: Boolean = false,
        ): Flow<AgentEvent> =
            flow {
                val providers = collectUsableProviders()
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
                val effectiveMaxIterations = agentSettings.effectiveMaxIterations
                val effectiveTemperature = agentSettings.effectiveTemperature

                val selectedRole = agentPlanner.selectRole(userText, is3DMode)
                val worldModel = if (projectId != null) worldModelStore.get(projectId) else null

                val messages = mutableListOf<ChatMessageDto>()
                messages.add(systemPromptMessage(selectedRole, worldModel))
                messages.addAll(conversationHistory)
                messages.add(userTurnMessage(userText, attachedImageBase64))

                var providerIndex = 0
                var iteration = 0
                var finished = false
                var turnPhase = TurnPhase.MAIN
                var mainTurnSummary: String? = null

                while (!finished && iteration < effectiveMaxIterations) {
                    iteration++
                    val provider = providers.getOrNull(providerIndex)
                    if (provider == null) {
                        emit(AgentEvent.Error("All configured providers failed or are exhausted.", isFatal = true))
                        return@flow
                    }

                    emit(AgentEvent.ThinkingStarted(provider.displayName))

                    val response = callProvider(provider, messages, effectiveTemperature.toDouble())

                    if (response == null) {
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

                    val choice = response.choices.firstOrNull()
                    if (choice == null) {
                        emit(AgentEvent.Error("Provider returned an empty response.", isFatal = false))
                        finished = true
                        continue
                    }

                    val assistantMessage = choice.message
                    messages.add(assistantMessage)

                    val toolCalls = assistantMessage.toolCalls
                    if (toolCalls.isNullOrEmpty()) {
                        val textContent =
                            assistantMessage.contentText
                                ?: assistantMessage.contentParts?.firstOrNull { it.type == "text" }?.text
                                ?: ""
                        if (textContent.isNotBlank()) {
                            emit(AgentEvent.AgentTextChunk(textContent))
                        }
                        finished = true
                        continue
                    }

                    var snapshotRequestedThisRound = false

                    for (toolCall in toolCalls) {
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
                                emit(AgentEvent.ToolCallFailed(toolCall.id, result.errorMessage))
                                messages.add(toolResultMessage(toolCall, "ERROR: ${result.errorMessage}"))
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
        private suspend fun collectUsableProviders(): List<AiProviderConfig> {
            val enabled = providerConfigRepository.configs.first().filter { it.isEnabled }
            val (withinQuota, exhausted) = enabled.partition { !it.isOverQuota }
            return withinQuota.sortedByDescending { it.isPrimary } +
                exhausted.sortedByDescending { it.isPrimary }
        }

        private fun systemPromptMessage(
            role: com.waheed.artificerx.core.agent.multiagent.AgentRole,
            worldModel: com.waheed.artificerx.core.agent.multiagent.WorldModel?,
        ): ChatMessageDto {
            val basePrompt =
                """
                You are the Reasoning Brain of ARTIFICER-X, an agentic art studio.
                You do not generate images directly. You create artwork and 3D
                sculptures exclusively by calling the provided tools.

                2D canvas tools: create_layer, delete_layer, set_active_layer,
                draw_path, draw_shape, apply_gradient, fill_region,
                set_layer_property, pick_color, apply_filter, add_text,
                create_mask, enable_symmetry, apply_pattern, draw_curve,
                import_image_layer, inspect_canvas.

                3D sculpting tools: create_primitive, sculpt_stroke, delete_mesh,
                set_mesh_color, transform_mesh, inspect_scene.

                Work in small deliberate steps. After any operation whose result
                you are not certain about, call inspect_canvas (2D) or
                inspect_scene (3D) to see the current state before continuing.
                When the requested work is complete, call finish_turn with a
                short summary. Never describe what you would create in plain
                text instead of calling a tool — plain text alone produces
                nothing on the canvas or in the scene.
                """.trimIndent()

            val roleSection = "\n\nCurrent specialist focus (${role.displayName}):\n${role.focusInstruction}"
            val worldModelSection = worldModel?.toPromptBlock()?.let { "\n\n$it" } ?: ""

            return ChatMessageDto(
                role = "system",
                contentText = basePrompt + roleSection + worldModelSection,
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

                val response = callProvider(provider, archivistMessages, temperature = 0.2)
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

        private suspend fun callProvider(
            provider: AiProviderConfig,
            messages: List<ChatMessageDto>,
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
                    tools = ToolRegistry.ALL_TOOLS,
                    temperature = temperature,
                    maxTokens = 2048,
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

        private fun defaultModelFor(provider: AiProviderConfig): String =
            when {
                provider.baseUrl.contains("groq", ignoreCase = true) -> "llama-3.2-90b-vision-preview"
                provider.baseUrl.contains("openrouter", ignoreCase = true) -> "meta-llama/llama-3.2-90b-vision-instruct:free"
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
