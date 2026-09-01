package com.waheed.artificerx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.local.datastore.AccessibilitySettings
import com.waheed.artificerx.data.local.datastore.AccessibilitySettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccessibilityViewModel
    @Inject
    constructor(
        private val dataStore: AccessibilitySettingsDataStore,
    ) : ViewModel() {
        val settings: StateFlow<AccessibilitySettings> =
            dataStore.settings.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AccessibilitySettings(),
            )

        fun setHighContrast(enabled: Boolean) = viewModelScope.launch { dataStore.setHighContrast(enabled) }

        fun setReduceMotion(enabled: Boolean) = viewModelScope.launch { dataStore.setReduceMotion(enabled) }

        fun setTextScale(scale: Float) = viewModelScope.launch { dataStore.setTextScale(scale) }

        fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch { dataStore.setHapticFeedback(enabled) }
    }
