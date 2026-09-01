package com.waheed.artificerx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.local.datastore.ProviderConfigRecord
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import com.waheed.artificerx.domain.model.AiProviderConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AiProvidersSettingsViewModel
    @Inject
    constructor(
        private val repository: ProviderConfigRepository,
    ) : ViewModel() {
        val providers: StateFlow<List<AiProviderConfig>> =
            repository.configs.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        fun toggleEnabled(provider: AiProviderConfig) {
            viewModelScope.launch {
                val record = provider.toRecordShape()
                repository.setEnabled(record, !provider.isEnabled)
            }
        }

        fun setPrimary(provider: AiProviderConfig) {
            viewModelScope.launch {
                repository.setPrimary(provider.toRecordShape())
            }
        }

        fun removeProvider(provider: AiProviderConfig) {
            viewModelScope.launch {
                repository.removeProvider(provider.id, provider.keyAlias)
            }
        }

        private fun AiProviderConfig.toRecordShape(): ProviderConfigRecord =
            ProviderConfigRecord(
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
