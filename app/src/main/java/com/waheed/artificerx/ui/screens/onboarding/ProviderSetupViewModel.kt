package com.waheed.artificerx.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.network.ConnectionTestResult
import com.waheed.artificerx.core.network.LLMAdapter
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import com.waheed.artificerx.domain.model.AiProviderPreset
import com.waheed.artificerx.domain.model.AiProviderPresets
import com.waheed.artificerx.domain.model.AiProviderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProviderSetupStep {
    CHOOSE_PRESET,
    ENTER_CREDENTIALS,
    TESTING,
    SUCCESS,
    FAILED,
}

data class ProviderSetupUiState(
    val step: ProviderSetupStep = ProviderSetupStep.CHOOSE_PRESET,
    val availablePresets: List<AiProviderPreset> = AiProviderPresets.ALL_PRESETS,
    val selectedPreset: AiProviderPreset? = null,
    val apiKeyInput: String = "",
    val accountIdInput: String = "",
    val customBaseUrlInput: String = "",
    val customDisplayNameInput: String = "",
    val isCustomProviderMode: Boolean = false,
    val apiKeyValidationError: String? = null,
    val connectionErrorMessage: String? = null,
    val configuredProviderCount: Int = 0,
    val isSubmitting: Boolean = false,
    val isFetchingModels: Boolean = false,
    val fetchedModels: List<com.waheed.artificerx.core.network.RemoteModelInfo> = emptyList(),
    val modelFetchError: String? = null,
    val selectedModelId: String? = null,
    val manualModelIdInput: String = "",
) {
    val canSubmit: Boolean
        get() {
            val keyOk = apiKeyInput.trim().length >= MIN_KEY_LENGTH
            val baseUrlOk = if (isCustomProviderMode) customBaseUrlInput.isNotBlank() else true
            val accountIdOk = if (selectedPreset?.requiresAccountId == true) accountIdInput.isNotBlank() else true
            return keyOk && baseUrlOk && accountIdOk && !isSubmitting
        }

    /** Section 196: models can only be fetched once we have enough to
     *  actually make the /models call — a base URL (preset or custom)
     *  and a key that at least looks well-formed. */
    val canFetchModels: Boolean
        get() {
            val keyOk = apiKeyInput.trim().length >= MIN_KEY_LENGTH
            val baseUrlOk = if (isCustomProviderMode) customBaseUrlInput.isNotBlank() else selectedPreset != null
            return keyOk && baseUrlOk && !isFetchingModels
        }

    /** Picking from the fetched list takes priority; the manual field is
     *  the fallback for providers whose /models endpoint is unreachable,
     *  non-standard, or simply empty. */
    val effectiveModelId: String?
        get() = selectedModelId ?: manualModelIdInput.trim().ifBlank { null }

    companion object {
        const val MIN_KEY_LENGTH = 8
    }
}

/**
 * Drives the Provider Setup onboarding flow end to end (Section 195-199,
 * 206, 211): pick a built-in preset OR configure a custom OpenAI-
 * compatible endpoint, enter/validate the key, run a live connection
 * test against the real provider before persisting anything, then save
 * through ProviderConfigRepository so the key lands in EncryptedKeyStore
 * and metadata lands in DataStore. Nothing is written until the
 * connection test succeeds — Section 191's "structured errors, not
 * opaque failures" principle applied to the very first setup a user
 * does.
 */
@HiltViewModel
class ProviderSetupViewModel
    @Inject
    constructor(
        private val repository: ProviderConfigRepository,
        private val llmAdapter: LLMAdapter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProviderSetupUiState())
        val uiState: StateFlow<ProviderSetupUiState> = _uiState.asStateFlow()

        fun selectPreset(preset: AiProviderPreset) {
            _uiState.update {
                it.copy(
                    step = ProviderSetupStep.ENTER_CREDENTIALS,
                    selectedPreset = preset,
                    isCustomProviderMode = false,
                    apiKeyInput = "",
                    accountIdInput = "",
                    apiKeyValidationError = null,
                    connectionErrorMessage = null,
                    fetchedModels = emptyList(),
                    modelFetchError = null,
                    selectedModelId = null,
                    manualModelIdInput = "",
                )
            }
        }

        fun selectCustomProvider() {
            _uiState.update {
                it.copy(
                    step = ProviderSetupStep.ENTER_CREDENTIALS,
                    selectedPreset = null,
                    isCustomProviderMode = true,
                    apiKeyInput = "",
                    customBaseUrlInput = "",
                    customDisplayNameInput = "",
                    apiKeyValidationError = null,
                    connectionErrorMessage = null,
                    fetchedModels = emptyList(),
                    modelFetchError = null,
                    selectedModelId = null,
                    manualModelIdInput = "",
                )
            }
        }

        fun onApiKeyChanged(value: String) {
            _uiState.update {
                it.copy(
                    apiKeyInput = value,
                    apiKeyValidationError =
                        if (value.isNotEmpty() && value.length < ProviderSetupUiState.MIN_KEY_LENGTH) {
                            "Key looks too short — double-check you copied the full value"
                        } else {
                            null
                        },
                )
            }
        }

        fun onAccountIdChanged(value: String) {
            _uiState.update { it.copy(accountIdInput = value) }
        }

        fun onCustomBaseUrlChanged(value: String) {
            _uiState.update { it.copy(customBaseUrlInput = value) }
        }

        fun onCustomDisplayNameChanged(value: String) {
            _uiState.update { it.copy(customDisplayNameInput = value) }
        }

        /**
         *  Builds the request base URL for a given preset + account ID.
         *
         *  Cloudflare Workers AI's OpenAI-compatible surface lives under
         *  `/accounts/{account_id}/ai/v1` (NOT `/ai/run`, which is
         *  Cloudflare's own non-OpenAI-shaped native invocation format —
         *  using it here would silently send every /chat/completions and
         *  /models call to a path that serves neither of those response
         *  shapes, and the connection test would look like a generic
         *  "unreachable" failure with no clear cause). Cloudflare's own
         *  docs document POST /ai/v1/chat/completions as explicitly
         *  "OpenAI SDK compatible."
         */
        private fun resolveEffectiveBaseUrl(
            baseUrl: String,
            type: AiProviderType,
            accountId: String?,
        ): String =
            if (type == AiProviderType.CLOUDFLARE_WORKERS_AI && accountId != null) {
                "${baseUrl.trimEnd('/')}/$accountId/ai/v1"
            } else {
                baseUrl
            }

        /** Section 196's live "Fetch Models" step: calls the provider's
         *  /models endpoint with whatever base URL + key are currently
         *  entered (works for both a built-in preset and a fully custom
         *  router) and populates a pickable list. Doesn't require the
         *  connection test to have run first — fetching models IS a
         *  connection test in practice, since a bad key/URL fails the same
         *  way here as it would in testAndSaveConnection. */
        fun fetchModels() {
            val state = _uiState.value
            if (!state.canFetchModels) return

            val baseUrl = resolveBaseUrl(state)
            val rawKey = state.apiKeyInput.trim()
            val accountId = state.accountIdInput.trim().ifBlank { null }
            val type = state.selectedPreset?.type ?: AiProviderType.CUSTOM
            val effectiveBaseUrl = resolveEffectiveBaseUrl(baseUrl, type, accountId)

            _uiState.update { it.copy(isFetchingModels = true, modelFetchError = null, fetchedModels = emptyList()) }

            viewModelScope.launch {
                val result = llmAdapter.listModels(baseUrl = effectiveBaseUrl, apiKey = rawKey, accountId = accountId)
                result.fold(
                    onSuccess = { models ->
                        _uiState.update {
                            it.copy(
                                isFetchingModels = false,
                                fetchedModels = models,
                                modelFetchError =
                                    if (models.isEmpty()) {
                                        "Provider returned no models — you can still enter a model name manually below."
                                    } else {
                                        null
                                    },
                            )
                        }
                    },
                    onFailure = { error ->
                        _uiState.update {
                            it.copy(
                                isFetchingModels = false,
                                fetchedModels = emptyList(),
                                modelFetchError =
                                    "Couldn't fetch models: ${error.message ?: "unknown error"}. " +
                                        "You can enter a model name manually below.",
                            )
                        }
                    },
                )
            }
        }

        fun onModelSelected(modelId: String) {
            _uiState.update { it.copy(selectedModelId = modelId, manualModelIdInput = "") }
        }

        fun onManualModelIdChanged(value: String) {
            _uiState.update { it.copy(manualModelIdInput = value, selectedModelId = null) }
        }

        fun backToPresetChoice() {
            _uiState.update {
                it.copy(step = ProviderSetupStep.CHOOSE_PRESET, connectionErrorMessage = null)
            }
        }

        fun testAndSaveConnection() {
            val state = _uiState.value
            if (!state.canSubmit) return

            val baseUrl = resolveBaseUrl(state)
            val displayName = resolveDisplayName(state)
            val type = state.selectedPreset?.type ?: AiProviderType.CUSTOM
            val rawKey = state.apiKeyInput.trim()
            val accountId = state.accountIdInput.trim().ifBlank { null }
            val effectiveBaseUrl = resolveEffectiveBaseUrl(baseUrl, type, accountId)

            _uiState.update { it.copy(step = ProviderSetupStep.TESTING, isSubmitting = true, connectionErrorMessage = null) }

            viewModelScope.launch {
                val result =
                    llmAdapter.testConnection(
                        baseUrl = effectiveBaseUrl,
                        apiKey = rawKey,
                        accountId = accountId,
                    )

                when (result) {
                    is ConnectionTestResult.Success -> {
                        repository.addProvider(
                            type = type,
                            displayName = displayName,
                            baseUrl = effectiveBaseUrl,
                            rawApiKey = rawKey,
                            supportsVision = state.selectedPreset?.supportsVision ?: true,
                            supportsToolCalling = state.selectedPreset?.supportsToolCalling ?: true,
                            knownDailyQuota = state.selectedPreset?.knownDailyQuota,
                            makePrimary = state.configuredProviderCount == 0,
                            defaultModelId = state.effectiveModelId,
                        )
                        _uiState.update {
                            it.copy(
                                step = ProviderSetupStep.SUCCESS,
                                isSubmitting = false,
                                configuredProviderCount = it.configuredProviderCount + 1,
                            )
                        }
                    }

                    is ConnectionTestResult.InvalidKey -> failWith(result.message)
                    is ConnectionTestResult.RateLimited ->
                        failWith(
                            "Rate limited by the provider" + (result.retryAfterSeconds?.let { " — retry in ${it}s" } ?: ""),
                        )
                    is ConnectionTestResult.Unreachable -> failWith("Couldn't reach the provider: ${result.message}")
                    is ConnectionTestResult.UnknownError -> failWith(result.message)
                }
            }
        }

        fun addAnotherProvider() {
            _uiState.update {
                it.copy(
                    step = ProviderSetupStep.CHOOSE_PRESET,
                    selectedPreset = null,
                    isCustomProviderMode = false,
                    apiKeyInput = "",
                    accountIdInput = "",
                    customBaseUrlInput = "",
                    customDisplayNameInput = "",
                    connectionErrorMessage = null,
                    isFetchingModels = false,
                    fetchedModels = emptyList(),
                    modelFetchError = null,
                    selectedModelId = null,
                    manualModelIdInput = "",
                )
            }
        }

        fun retryAfterFailure() {
            _uiState.update { it.copy(step = ProviderSetupStep.ENTER_CREDENTIALS, connectionErrorMessage = null, isSubmitting = false) }
        }

        private fun failWith(message: String) {
            _uiState.update {
                it.copy(step = ProviderSetupStep.FAILED, isSubmitting = false, connectionErrorMessage = message)
            }
        }

        private fun resolveBaseUrl(state: ProviderSetupUiState): String =
            if (state.isCustomProviderMode) {
                state.customBaseUrlInput.trim()
            } else {
                state.selectedPreset?.defaultBaseUrl.orEmpty()
            }

        private fun resolveDisplayName(state: ProviderSetupUiState): String =
            if (state.isCustomProviderMode) {
                state.customDisplayNameInput.trim().ifBlank { "Custom Provider" }
            } else {
                state.selectedPreset?.displayName.orEmpty()
            }
    }
