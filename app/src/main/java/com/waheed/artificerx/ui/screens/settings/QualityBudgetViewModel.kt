package com.waheed.artificerx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.local.datastore.AgentSettings
import com.waheed.artificerx.data.local.datastore.AgentSettingsDataStore
import com.waheed.artificerx.data.local.datastore.QualityPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QualityBudgetViewModel
    @Inject
    constructor(
        private val agentSettingsDataStore: AgentSettingsDataStore,
    ) : ViewModel() {
        val settings: StateFlow<AgentSettings> =
            agentSettingsDataStore.settings.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AgentSettings(),
            )

        fun selectPreset(preset: QualityPreset) {
            viewModelScope.launch {
                agentSettingsDataStore.setPreset(preset)
            }
        }
    }
