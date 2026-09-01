package com.waheed.artificerx.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.agent.LocalGenerationResult
import com.waheed.artificerx.core.agent.LocalInferenceEngine
import com.waheed.artificerx.data.repository.GgufImportResult
import com.waheed.artificerx.data.repository.LocalModelRepository
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import com.waheed.artificerx.domain.model.AiProviderType
import com.waheed.artificerx.domain.model.LocalModelInfo
import com.waheed.artificerx.domain.model.LocalModelLoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Two-stage staging area the import flow moves through: pick base
 *  model → (optionally) pick mmproj → name it → confirm. Modeled as
 *  explicit state rather than a multi-screen wizard since the whole
 *  flow fits comfortably in one bottom sheet. */
data class LocalModelImportDraft(
    val baseModelResult: GgufImportResult.Success? = null,
    val mmprojResult: GgufImportResult.Success? = null,
    val displayName: String = "",
    val contextLength: Int = LocalModelInfo.DEFAULT_CONTEXT_LENGTH,
    val threadCount: Int = LocalModelInfo.DEFAULT_THREAD_COUNT,
    val lastError: String? = null,
    val isImporting: Boolean = false,
) {
    val canConfirm: Boolean get() = baseModelResult != null && !isImporting
}

data class LocalModelUiState(
    val models: List<LocalModelInfo> = emptyList(),
    val activeModelId: String? = null,
    val loadState: LocalModelLoadState = LocalModelLoadState.NOT_LOADED,
    val loadedModelId: String? = null,
    val importDraft: LocalModelImportDraft? = null,
    val testPrompt: String = "",
    val testOutput: String = "",
    val isTesting: Boolean = false,
)

/**
 * Backs the Local Model settings screen (Section: Local Model provider
 * — "mera khud ka local model jiski files mere paas hongi wo upload
 * karke chala sako, saath hi mmproj bhi"). Coordinates three
 * collaborators that each own one slice of this feature:
 *  - LocalModelRepository: import/validate/persist GGUF + mmproj files
 *  - LocalInferenceEngine: the actual load/generate lifecycle, shared
 *    with AgentOrchestrator so "test this model" here and "use this
 *    model in chat" elsewhere are backed by the exact same engine
 *    instance (no separate, divergent load path for the settings UI)
 *  - ProviderConfigRepository: registers/removes a lightweight
 *    AiProviderType.LOCAL_GGUF entry so the agent's provider list
 *    (fallback ordering, enable/disable, "primary brain" star) treats
 *    the active local model exactly like any remote provider
 */
@HiltViewModel
class LocalModelViewModel
    @Inject
    constructor(
        private val localModelRepository: LocalModelRepository,
        private val providerConfigRepository: ProviderConfigRepository,
        private val inferenceEngine: LocalInferenceEngine,
    ) : ViewModel() {
        private val _importDraft = MutableStateFlow<LocalModelImportDraft?>(null)
        private val _testPrompt = MutableStateFlow("")
        private val _testOutput = MutableStateFlow("")
        private val _isTesting = MutableStateFlow(false)

        val uiState: StateFlow<LocalModelUiState> =
            combine(
                localModelRepository.importedModels,
                localModelRepository.activeModelId,
                inferenceEngine.loadState,
                inferenceEngine.loadedModelId,
                _importDraft,
            ) { models, activeId, loadState, loadedId, draft ->
                LocalModelUiState(
                    models = models,
                    activeModelId = activeId,
                    loadState = loadState,
                    loadedModelId = loadedId,
                    importDraft = draft,
                    testPrompt = _testPrompt.value,
                    testOutput = _testOutput.value,
                    isTesting = _isTesting.value,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LocalModelUiState())

        // ── Import flow ──

        fun buildPickerIntent() = localModelRepository.buildOpenDocumentIntent()

        fun startImportDraft() {
            _importDraft.value = LocalModelImportDraft()
        }

        fun cancelImportDraft() {
            _importDraft.value = null
        }

        fun onBaseModelPicked(uri: Uri) {
            val draft = _importDraft.value ?: LocalModelImportDraft()
            _importDraft.value = draft.copy(isImporting = true, lastError = null)
            viewModelScope.launch {
                when (val result = localModelRepository.importBaseModel(uri)) {
                    is GgufImportResult.Success -> {
                        val current = _importDraft.value
                        _importDraft.value =
                            current?.copy(
                                baseModelResult = result,
                                displayName = current.displayName.ifBlank { suggestedName(result.fileName) },
                                isImporting = false,
                            )
                    }
                    else -> {
                        _importDraft.value =
                            _importDraft.value?.copy(
                                isImporting = false,
                                lastError = result.toUserMessage("base model"),
                            )
                    }
                }
            }
        }

        fun onMmprojPicked(uri: Uri) {
            val draft = _importDraft.value ?: return
            _importDraft.value = draft.copy(isImporting = true, lastError = null)
            viewModelScope.launch {
                when (val result = localModelRepository.importMmprojFile(uri)) {
                    is GgufImportResult.Success -> {
                        _importDraft.value = _importDraft.value?.copy(mmprojResult = result, isImporting = false)
                    }
                    else -> {
                        _importDraft.value =
                            _importDraft.value?.copy(
                                isImporting = false,
                                lastError = result.toUserMessage("vision projector (mmproj)"),
                            )
                    }
                }
            }
        }

        fun clearMmproj() {
            _importDraft.value = _importDraft.value?.copy(mmprojResult = null)
        }

        fun updateDraftName(name: String) {
            _importDraft.value = _importDraft.value?.copy(displayName = name)
        }

        fun updateDraftContextLength(contextLength: Int) {
            _importDraft.value = _importDraft.value?.copy(contextLength = contextLength)
        }

        fun updateDraftThreadCount(threadCount: Int) {
            _importDraft.value = _importDraft.value?.copy(threadCount = threadCount)
        }

        fun confirmImport() {
            val draft = _importDraft.value ?: return
            val baseResult = draft.baseModelResult ?: return
            viewModelScope.launch {
                val registered =
                    localModelRepository.registerModel(
                        modelResult = baseResult,
                        mmprojResult = draft.mmprojResult,
                        displayName = draft.displayName,
                        contextLength = draft.contextLength,
                        threadCount = draft.threadCount,
                    )
                ensureProviderRegistered(registered)
                _importDraft.value = null
            }
        }

        // ── Model management ──

        fun setActiveModel(model: LocalModelInfo) {
            viewModelScope.launch {
                localModelRepository.setActiveModel(model.id)
                ensureProviderRegistered(model)
            }
        }

        fun removeModel(model: LocalModelInfo) {
            viewModelScope.launch {
                if (inferenceEngine.loadedModelId.value == model.id) {
                    inferenceEngine.unload()
                }
                localModelRepository.removeModel(model.id)
            }
        }

        fun updateSettings(
            model: LocalModelInfo,
            contextLength: Int? = null,
            threadCount: Int? = null,
            temperature: Float? = null,
            topK: Int? = null,
            topP: Float? = null,
            minP: Float? = null,
            useGpuOffload: Boolean? = null,
        ) {
            viewModelScope.launch {
                localModelRepository.updateModelSettings(
                    modelId = model.id,
                    contextLength = contextLength,
                    threadCount = threadCount,
                    temperature = temperature,
                    topK = topK,
                    topP = topP,
                    minP = minP,
                    useGpuOffloadIfAvailable = useGpuOffload,
                )
            }
        }

        // ── Load & test ──

        fun loadModel(model: LocalModelInfo) {
            viewModelScope.launch {
                inferenceEngine.loadModel(model)
            }
        }

        fun unloadModel() {
            inferenceEngine.unload()
        }

        fun updateTestPrompt(text: String) {
            _testPrompt.value = text
        }

        fun runTestGeneration(model: LocalModelInfo) {
            val prompt = _testPrompt.value
            if (prompt.isBlank()) return
            viewModelScope.launch {
                _isTesting.value = true
                _testOutput.value = ""
                val loaded = inferenceEngine.loadModel(model)
                if (!loaded) {
                    _testOutput.value = "Failed to load model — check the load state for details."
                    _isTesting.value = false
                    return@launch
                }
                when (val result = inferenceEngine.generateToCompletion(prompt)) {
                    is LocalGenerationResult.Success -> _testOutput.value = result.fullText
                    is LocalGenerationResult.Failure -> _testOutput.value = "Error: ${result.message}"
                    is LocalGenerationResult.Aborted -> _testOutput.value = "Generation timed out or was aborted."
                }
                _isTesting.value = false
            }
        }

        fun abortTest() {
            inferenceEngine.abort()
            _isTesting.value = false
        }

        // ── Internal helpers ──

        /** Keeps exactly one AiProviderType.LOCAL_GGUF entry in
         *  ProviderConfigRepository, pointed at whichever LocalModelInfo
         *  is currently active — so AgentOrchestrator's provider list
         *  always reflects the user's current local-model choice without
         *  accumulating a stale duplicate entry per import. */
        private suspend fun ensureProviderRegistered(model: LocalModelInfo) {
            val existing = providerConfigRepository.configs.first().firstOrNull { it.type == AiProviderType.LOCAL_GGUF }
            if (existing != null) {
                providerConfigRepository.removeProvider(existing.id, existing.keyAlias)
            }
            providerConfigRepository.addProvider(
                type = AiProviderType.LOCAL_GGUF,
                displayName = "Local: ${model.displayName}",
                baseUrl = "",
                rawApiKey = "local-no-key-required",
                supportsVision = model.isVisionCapable,
                supportsToolCalling = true,
                knownDailyQuota = null,
                makePrimary = false,
                defaultModelId = model.id,
            )
        }

        private fun suggestedName(fileName: String): String =
            fileName
                .removeSuffix(".gguf")
                .replace('_', ' ')
                .replace('-', ' ')
                .trim()

        private fun GgufImportResult.toUserMessage(context: String): String =
            when (this) {
                is GgufImportResult.InvalidHeader -> "That file doesn't look like a valid GGUF $context — check you picked the right file."
                is GgufImportResult.FileTooSmallToRead -> "That file is too small to be a real GGUF $context."
                is GgufImportResult.PermissionDenied -> "Couldn't get permanent access to that file: $message"
                is GgufImportResult.UnknownError -> "Import failed: $message"
                is GgufImportResult.Success -> ""
            }
    }
