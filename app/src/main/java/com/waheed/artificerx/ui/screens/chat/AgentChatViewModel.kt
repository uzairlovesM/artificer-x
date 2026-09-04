package com.waheed.artificerx.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.agent.AgentEvent
import com.waheed.artificerx.core.chat.ChatProfile
import com.waheed.artificerx.core.chat.ChatProfileStore
import com.waheed.artificerx.core.agent.AgentOrchestrator
import com.waheed.artificerx.core.export.ImageExporter
import com.waheed.artificerx.core.network.ChatMessageDto
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import com.waheed.artificerx.data.local.datastore.ChatSessionDataStore
import com.waheed.artificerx.domain.model.AgentActivityState
import com.waheed.artificerx.domain.model.ChatMessage
import com.waheed.artificerx.domain.model.ChatMessageRole
import com.waheed.artificerx.domain.model.ToolCallEntry
import com.waheed.artificerx.domain.model.ToolCallStatus
import com.waheed.artificerx.ui.screens.canvas.StudioViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AgentChatUiState(
    val activeThreadId: String = "",
    val threadTitles: Map<String, String> = emptyMap(),
    val artifactCount: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isAgentResponding: Boolean = false,
    val hasConfiguredProvider: Boolean = true,
    val attachedImageUri: String? = null,
    val attachedImageBase64: String? = null,
    val lastErrorMessage: String? = null,
    // Section: Local Model provider — surfaced from AgentEvent.LocalModelLoading
    // while the on-device GGUF engine is warming up. Null once loading finishes
    // (a LocalModelSpeed or any non-loading event clears it).
    val localModelLoadingPhase: String? = null,
    val localModelLoadingProgress: Float? = null,
    // Section: Local Model provider — latest tok/s sample from
    // AgentEvent.LocalModelSpeed, shown as a small live throughput indicator.
    val localModelTokensPerSecond: Double? = null,
    val chatProfiles: List<ChatProfile> = emptyList(),
    val activeProfileId: String? = null,
)

/**
 * Real conversational entry point to AgentOrchestrator (Section 90/91,
 * 155). Every AgentEvent the orchestrator emits gets mapped onto the
 * chat message list in real time: a new tool-call chip appears the
 * instant a tool_call starts, flips to success/failure as its result
 * comes back, and the agent's final text or finish_turn summary lands
 * as a normal agent bubble. Requires an active StudioViewModel
 * instance to execute tools against — shared from StudioScreen's nav
 * graph scope so canvas mutations are visible immediately when the
 * user returns to the Studio.
 */
@HiltViewModel
class AgentChatViewModel
    @Inject
    constructor(
        private val providerConfigRepository: ProviderConfigRepository,
        private val agentOrchestrator: AgentOrchestrator,
        private val imageExporter: ImageExporter,
        private val workspaceRepository: com.waheed.artificerx.data.repository.ChatWorkspaceRepository,
        private val chatSessionDataStore: ChatSessionDataStore,
        private val responseArtifactMaterializer: com.waheed.artificerx.core.agent.AIResponseArtifactMaterializer,
        private val chatProfileStore: ChatProfileStore,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AgentChatUiState())
        init {
            viewModelScope.launch { chatProfileStore.profiles.collect { profiles -> _uiState.update { it.copy(chatProfiles = profiles) } } }
            viewModelScope.launch { chatProfileStore.activeProfileId.collect { id -> _uiState.update { it.copy(activeProfileId = id) } } }
        }
        val uiState: StateFlow<AgentChatUiState> = _uiState.asStateFlow()

        private var linkedStudioViewModel: StudioViewModel? = null
        private var linkedSculptViewModel: com.waheed.artificerx.ui.screens.sculpt.SculptViewModel? = null

        // Section: stopping/cancellation. Tracks the coroutine Job running
        // the current agent turn's Flow<AgentEvent> collection so
        // stopCurrentTurn() has something real to cancel — previously
        // there was no reference to this Job anywhere, and no way for the
        // user to interrupt a turn already in flight (only the automatic
        // MAX_ITERATIONS safety limit could end one early).
        private var currentTurnJob: kotlinx.coroutines.Job? = null

        private val persistJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

        init {
            viewModelScope.launch {
                workspaceRepository.observeThreads().collect { threads ->
                    _uiState.update { state -> state.copy(threadTitles = threads.associate { it.id to it.title }) }
                    if (_uiState.value.activeThreadId.isBlank()) {
                        val savedId = chatSessionDataStore.getActiveThreadId()
                        val savedThreadStillExists = savedId?.let { id -> threads.any { it.id == id } } == true
                        val id = if (savedThreadStillExists) savedId!! else (threads.firstOrNull()?.id ?: workspaceRepository.createThread())
                        if (!savedThreadStillExists) chatSessionDataStore.setActiveThreadId(id)
                        val messages = workspaceRepository.loadMessages(id)
                        val artifactCount = workspaceRepository.observeArtifacts(id).first().size
                        _uiState.update { it.copy(activeThreadId = id, messages = messages, artifactCount = artifactCount) }
                    }
                }
            }
            viewModelScope.launch {
                val hasProvider = providerConfigRepository.hasAnyProviderConfigured()
                _uiState.update { it.copy(hasConfiguredProvider = hasProvider) }
            }
            viewModelScope.launch {
                _uiState
                    .map { it.activeThreadId }
                    .filter(String::isNotBlank)
                    .distinctUntilChanged()
                    .flatMapLatest { workspaceRepository.observeArtifacts(it) }
                    .collect { artifacts -> _uiState.update { state -> state.copy(artifactCount = artifacts.size) } }
            }
        }

        fun switchThread(threadId: String) {
            if (threadId.isBlank() || threadId == _uiState.value.activeThreadId) return
            viewModelScope.launch {
                val messages = workspaceRepository.loadMessages(threadId)
                val artifactCount = workspaceRepository.observeArtifacts(threadId).first().size
                chatSessionDataStore.setActiveThreadId(threadId)
                val profileId = chatProfileStore.getProfileForThread(threadId)
                _uiState.update { it.copy(activeThreadId = threadId, messages = messages, artifactCount = artifactCount, inputText = "", activeProfileId = profileId) }
            }
        }

        fun newThread() {
            viewModelScope.launch {
                val id = workspaceRepository.createThread()
                chatSessionDataStore.setActiveThreadId(id)
                val profileId = chatProfileStore.getProfileForThread(id)
                _uiState.update { it.copy(activeThreadId = id, messages = emptyList(), inputText = "", artifactCount = 0, activeProfileId = profileId) }
            }
        }

        fun deleteActiveThread() {
            val id = _uiState.value.activeThreadId
            if (id.isBlank()) return
            viewModelScope.launch {
                workspaceRepository.deleteThreadForever(id)
                val fresh = workspaceRepository.createThread()
                chatSessionDataStore.setActiveThreadId(fresh)
                _uiState.update { it.copy(activeThreadId = fresh, messages = emptyList(), artifactCount = 0) }
            }
        }

        /** Called once from AgentChatScreen with the shared StudioViewModel
         *  instance so tool calls land on the actual visible canvas. */
        fun bindStudioViewModel(studioViewModel: StudioViewModel) {
            linkedStudioViewModel = studioViewModel
            linkedSculptViewModel = null
        }

        fun bindSculptViewModel(sculptViewModel: com.waheed.artificerx.ui.screens.sculpt.SculptViewModel) {
            linkedSculptViewModel = sculptViewModel
            linkedStudioViewModel = null
        }

        fun setActiveProfile(id: String) { viewModelScope.launch {
            val thread = _uiState.value.activeThreadId
            if(thread.isNotBlank()) chatProfileStore.setProfileForThread(thread, id) else chatProfileStore.setActive(id)
            _uiState.update { it.copy(activeProfileId = id) }
        } }

        fun saveProfile(profile: ChatProfile) { viewModelScope.launch {
            val current = _uiState.value.chatProfiles.toMutableList()
            val index = current.indexOfFirst { it.id==profile.id }
            if(index>=0) current[index] = profile else current.add(profile)
            chatProfileStore.saveProfiles(current)
        } }

        fun deleteProfile(id: String) { viewModelScope.launch {
            val remaining = _uiState.value.chatProfiles.filterNot { it.id==id }
            if(remaining.isNotEmpty()) chatProfileStore.saveProfiles(remaining)
        } }

        fun onInputChanged(text: String) {
            _uiState.update { it.copy(inputText = text) }
        }

        fun onImageAttached(
            uri: String?,
            base64: String?,
        ) {
            _uiState.update { it.copy(attachedImageUri = uri, attachedImageBase64 = base64) }
        }

        fun sendMessage() {
            val state = _uiState.value
            val text = state.inputText.trim()
            if (text.isEmpty() || state.isAgentResponding) return

            val studioViewModel = linkedStudioViewModel
            val sculptViewModel = linkedSculptViewModel
            if (studioViewModel == null && sculptViewModel == null) {
                _uiState.update { it.copy(lastErrorMessage = "No active session — open Studio or Sculpt Studio first.") }
                return
            }

            val userMessage =
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = ChatMessageRole.USER,
                    text = text,
                    attachedImageUri = state.attachedImageUri,
                )

            val agentMessageId = UUID.randomUUID().toString()
            val agentPlaceholder =
                ChatMessage(
                    id = agentMessageId,
                    role = ChatMessageRole.AGENT,
                    text = "",
                    isStreaming = true,
                )

            _uiState.update {
                it.copy(
                    messages = it.messages + userMessage + agentPlaceholder,
                    inputText = "",
                    attachedImageUri = null,
                    attachedImageBase64 = null,
                    isAgentResponding = true,
                    lastErrorMessage = null,
                )
            }

            viewModelScope.launch {
                if (state.messages.none { it.role == ChatMessageRole.USER }) {
                    workspaceRepository.renameThread(state.activeThreadId, text.take(52).replace("\n", " "))
                }
                workspaceRepository.saveMessage(_uiState.value.activeThreadId, userMessage)
                workspaceRepository.saveMessage(_uiState.value.activeThreadId, agentPlaceholder)
            }

            studioViewModel?.setAgentActivity(AgentActivityState.THINKING)
            sculptViewModel?.setAgentActivity(AgentActivityState.THINKING)

            val attachedBitmap =
                state.attachedImageBase64?.let { base64 ->
                    runCatching {
                        val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                }
            studioViewModel?.setPendingAttachedImage(attachedBitmap)

            val history = buildConversationHistoryDto(state.messages)

            currentTurnJob =
                viewModelScope.launch {
                    val snapshotProvider =
                        com.waheed.artificerx.core.agent.SnapshotProvider {
                            studioViewModel?.captureSnapshotNow() ?: sculptViewModel?.captureSnapshotNow()
                        }

                    runCatching {
                        agentOrchestrator
                            .handleUserMessage(
                                userText = text,
                                attachedImageBase64 = state.attachedImageBase64,
                                conversationHistory = history,
                                studioViewModel = studioViewModel,
                                snapshotProvider = snapshotProvider,
                                projectId = studioViewModel?.state?.value?.projectId,
                                is3DMode = studioViewModel == null && sculptViewModel != null,
                                chatProfile = state.chatProfiles.firstOrNull { it.id == state.activeProfileId },
                            ).collect { event ->
                                applyAgentEvent(event, agentMessageId, studioViewModel, sculptViewModel)
                            }
                    }.onFailure { error ->
                        // A cancellation from stopCurrentTurn() surfaces here as
                        // a CancellationException — that's the expected,
                        // user-requested path, not a real failure, so it's
                        // deliberately not shown as an error message. Any other
                        // exception (a genuine crash mid-turn) still gets
                        // surfaced so the user isn't left staring at a
                        // silently-stuck "thinking" bubble.
                        if (error !is kotlinx.coroutines.CancellationException) {
                            _uiState.update {
                                it.copy(lastErrorMessage = "Agent turn failed: ${error.message ?: "unknown error"}")
                            }
                        }
                    }

                    updateAgentMessage(agentMessageId) { message ->
                        if (message.isStreaming) message.copy(isStreaming = false) else message
                    }
                    _uiState.update { it.copy(isAgentResponding = false) }
                    studioViewModel?.setAgentActivity(AgentActivityState.IDLE)
                    sculptViewModel?.setAgentActivity(AgentActivityState.IDLE)
                    currentTurnJob = null
                }
        }

        /** Section: stopping/cancellation. Cancels the in-flight agent
         *  turn's Job — this stops the Flow<AgentEvent> collection
         *  immediately, which in turn cancels whatever suspend call
         *  AgentOrchestrator is currently awaiting (an in-progress OkHttp
         *  request via runCatching's coroutine cancellation propagation,
         *  or a local-model generation via LocalInferenceEngine's own
         *  cancellation handling). Marks the in-progress agent bubble as
         *  no-longer-streaming with a clear "Stopped by user" note rather
         *  than leaving it looking like it's still thinking. */
        fun stopCurrentTurn() {
            val job = currentTurnJob ?: return
            job.cancel()
            currentTurnJob = null
            updateAgentMessage(lastAgentMessageId() ?: return) { message ->
                message.copy(
                    isStreaming = false,
                    text = message.text.ifBlank { "Stopped by user." },
                )
            }
            _uiState.update { it.copy(isAgentResponding = false) }
            linkedStudioViewModel?.setAgentActivity(AgentActivityState.IDLE)
            linkedSculptViewModel?.setAgentActivity(AgentActivityState.IDLE)
        }

        private fun lastAgentMessageId(): String? =
            _uiState.value.messages
                .lastOrNull { it.role == ChatMessageRole.AGENT }
                ?.id

        private suspend fun applyAgentEvent(
            event: AgentEvent,
            agentMessageId: String,
            studioViewModel: StudioViewModel?,
            sculptViewModel: com.waheed.artificerx.ui.screens.sculpt.SculptViewModel?,
        ) {
            when (event) {
                is AgentEvent.ThinkingStarted -> {
                    studioViewModel?.setAgentActivity(AgentActivityState.THINKING)
                    sculptViewModel?.setAgentActivity(AgentActivityState.THINKING)
                }

                is AgentEvent.ToolCallStarted -> {
                    studioViewModel?.setAgentActivity(AgentActivityState.CALLING_TOOL)
                    sculptViewModel?.setAgentActivity(AgentActivityState.CALLING_TOOL)
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(
                            toolCalls =
                                message.toolCalls +
                                    ToolCallEntry(
                                        id = event.callId,
                                        toolName = event.toolName,
                                        argsPreview = event.argsPreview,
                                        status = ToolCallStatus.RUNNING,
                                    ),
                        )
                    }
                }

                is AgentEvent.ToolCallSucceeded -> {
                    val mediaUri = Regex("MEDIA_URI=([^\n]+)").find(event.resultSummary)?.groupValues?.getOrNull(1)?.trim()
                    val artifactName = Regex("ARTIFACT_NAME=([^\n]+)").find(event.resultSummary)?.groupValues?.getOrNull(1)?.trim()
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(
                            autoSavedFileName = artifactName ?: message.autoSavedFileName,
                            autoSavedUri = mediaUri?.let(android.net.Uri::parse) ?: message.autoSavedUri,
                            toolCalls =
                                message.toolCalls.map {
                                    if (it.id ==
                                        event.callId
                                    ) {
                                        it.copy(status = ToolCallStatus.SUCCESS, resultSummary = event.resultSummary)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                }

                is AgentEvent.ToolCallFailed -> {
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(
                            toolCalls =
                                message.toolCalls.map {
                                    if (it.id ==
                                        event.callId
                                    ) {
                                        it.copy(status = ToolCallStatus.FAILED, resultSummary = event.errorMessage)
                                    } else {
                                        it
                                    }
                                },
                        )
                    }
                }

                is AgentEvent.AgentTextChunk -> {
                    // v0.4.30: text now arrives as real incremental deltas
                    // (see AgentOrchestrator.streamCloudProvider), so
                    // isStreaming stays true while chunks are still coming
                    // in — TurnCompleted below is what actually closes it
                    // out. Previously this flipped to false on the very
                    // first (and only) chunk, which was fine when that
                    // chunk WAS the whole reply, but no longer matches
                    // reality now that a reply can arrive over dozens of
                    // chunks.
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(text = message.text + event.text, isStreaming = true)
                    }
                }

                is AgentEvent.TurnCompleted -> {
                    studioViewModel?.setAgentActivity(AgentActivityState.IDLE)
                    sculptViewModel?.setAgentActivity(AgentActivityState.IDLE)
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(
                            text = if (message.text.isBlank()) event.summary else message.text,
                            isStreaming = false,
                        )
                    }
                    val completedText = _uiState.value.messages.firstOrNull { it.id == agentMessageId }?.text.orEmpty()
                    val materialized = responseArtifactMaterializer.materialize(
                        threadId = _uiState.value.activeThreadId,
                        response = completedText,
                    )
                    if (materialized.isNotEmpty()) {
                        updateAgentMessage(agentMessageId) { message ->
                            message.copy(
                                toolCalls = message.toolCalls + materialized.map { artifact ->
                                    ToolCallEntry(
                                        id = UUID.randomUUID().toString(),
                                        toolName = "response_artifact",
                                        argsPreview = artifact.name,
                                        status = ToolCallStatus.SUCCESS,
                                        resultSummary = "Created artifact ${artifact.name} at ${artifact.path}",
                                    )
                                },
                            )
                        }
                    }

                    // Auto-save (v0.4.30): every completed AI turn that touched
                    // the 2D canvas gets its result written straight to
                    // Pictures/ARTIFICER-X automatically — the user should
                    // never have to remember to hit Export to see or keep
                    // what the agent just drew. Runs on a timestamped name so
                    // repeated turns never overwrite each other. Failures are
                    // surfaced (not swallowed) but never block the chat UI.
                    if (studioViewModel != null) {
                        val snapshot = studioViewModel.captureSnapshotNow()
                        val timestamp =
                            java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
                                .format(java.util.Date())
                        when (val result = imageExporter.exportPng(snapshot, "ArtificerX_AI_$timestamp")) {
                            is com.waheed.artificerx.core.export.ExportResult.Success ->
                                updateAgentMessage(agentMessageId) { message ->
                                    message.copy(autoSavedFileName = result.displayName, autoSavedUri = result.uri)
                                }
                            is com.waheed.artificerx.core.export.ExportResult.Failure ->
                                _uiState.update {
                                    it.copy(lastErrorMessage = "AI output auto-save failed: ${result.message}")
                                }
                        }
                    }
                }

                is AgentEvent.ProviderFallback -> {
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(
                            toolCalls =
                                message.toolCalls +
                                    ToolCallEntry(
                                        id = UUID.randomUUID().toString(),
                                        toolName = "provider_fallback",
                                        argsPreview = "${event.fromProvider} → ${event.toProvider}",
                                        status = ToolCallStatus.SUCCESS,
                                        resultSummary = event.reason,
                                    ),
                        )
                    }
                }

                is AgentEvent.CriticReview -> {
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(
                            toolCalls =
                                message.toolCalls +
                                    ToolCallEntry(
                                        id = UUID.randomUUID().toString(),
                                        toolName = "critic_review",
                                        argsPreview = if (event.approved) "Approved" else "Needs repair",
                                        status = if (event.approved) ToolCallStatus.SUCCESS else ToolCallStatus.FAILED,
                                        resultSummary = event.detail,
                                    ),
                        )
                    }
                }

                is AgentEvent.RepairStarted -> {
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(
                            toolCalls =
                                message.toolCalls +
                                    ToolCallEntry(
                                        id = UUID.randomUUID().toString(),
                                        toolName = "repair_pass",
                                        argsPreview = "Fixing flagged issue",
                                        status = ToolCallStatus.RUNNING,
                                        resultSummary = event.issue,
                                    ),
                        )
                    }
                }

                is AgentEvent.Error -> {
                    if (event.isFatal) {
                        studioViewModel?.setAgentActivity(AgentActivityState.ERROR)
                        sculptViewModel?.setAgentActivity(AgentActivityState.ERROR)
                        updateAgentMessage(agentMessageId) { message ->
                            message.copy(text = event.message, isStreaming = false)
                        }
                    } else {
                        // Non-fatal — a heads-up (e.g. low battery) that
                        // shouldn't be mistaken for the turn having
                        // stopped. Surfaced via lastErrorMessage (a
                        // dismissible snackbar-style banner in
                        // AgentChatScreen) without touching the agent
                        // bubble's isStreaming/text state, so the turn
                        // visibly keeps going.
                    }
                    _uiState.update { it.copy(lastErrorMessage = event.message) }
                }

                is AgentEvent.MaxIterationsReached -> {
                    updateAgentMessage(agentMessageId) { message ->
                        message.copy(
                            text =
                                message.text.ifBlank {
                                    "Stopped after reaching the safety limit on tool-call iterations for this turn."
                                },
                            isStreaming = false,
                        )
                    }
                }

                is AgentEvent.LocalModelLoading -> {
                    _uiState.update {
                        it.copy(
                            localModelLoadingPhase = event.phase,
                            localModelLoadingProgress = event.progressFraction,
                        )
                    }
                }

                is AgentEvent.LocalModelSpeed -> {
                    // Loading is over once tokens are actually being generated —
                    // clear the loading phase so the UI swaps from the loading
                    // indicator to the tok/s readout.
                    _uiState.update {
                        it.copy(
                            localModelLoadingPhase = null,
                            localModelLoadingProgress = null,
                            localModelTokensPerSecond = event.tokensPerSecond,
                        )
                    }
                }
            }
        }

        private fun updateAgentMessage(
            messageId: String,
            transform: (ChatMessage) -> ChatMessage,
        ) {
            var changed: ChatMessage? = null
            var threadId = ""
            _uiState.update { state ->
                val updated = state.messages.map { message -> if (message.id == messageId) transform(message) else message }
                changed = updated.firstOrNull { it.id == messageId }
                threadId = state.activeThreadId
                state.copy(messages = updated)
            }
            changed?.let { message ->
                persistJobs[messageId]?.cancel()
                val immediate = !message.isStreaming
                persistJobs[messageId] = viewModelScope.launch {
                    if (!immediate) kotlinx.coroutines.delay(350)
                    workspaceRepository.saveMessage(threadId, message)
                    if (immediate) persistJobs.remove(messageId)
                }
            }
        }

        private fun buildConversationHistoryDto(messages: List<ChatMessage>): List<ChatMessageDto> =
            messages
                .filter { it.role == ChatMessageRole.USER || it.role == ChatMessageRole.AGENT }
                .filter { it.text.isNotBlank() }
                .map { message ->
                    ChatMessageDto(
                        role = if (message.role == ChatMessageRole.USER) "user" else "assistant",
                        contentText = message.text,
                    )
                }

        fun clearConversation() {
            deleteActiveThread()
        }

        fun dismissError() {
            _uiState.update { it.copy(lastErrorMessage = null) }
        }

        override fun onCleared() {
            currentTurnJob?.cancel()
            persistJobs.values.forEach { it.cancel() }
            persistJobs.clear()
            super.onCleared()
        }
    }
