package com.waheed.artificerx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import com.waheed.artificerx.domain.model.AiProviderConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class CostVisibilityViewModel
    @Inject
    constructor(
        repository: ProviderConfigRepository,
    ) : ViewModel() {
        val providers: StateFlow<List<AiProviderConfig>> =
            repository.configs.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )
    }
