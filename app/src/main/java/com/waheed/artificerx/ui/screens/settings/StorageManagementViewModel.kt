package com.waheed.artificerx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.repository.StorageStats
import com.waheed.artificerx.data.repository.StorageStatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val stats: StorageStats? = null,
    val isLoading: Boolean = true,
)

@HiltViewModel
class StorageManagementViewModel
    @Inject
    constructor(
        private val storageStatsRepository: StorageStatsRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(StorageUiState())
        val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

        init {
            refresh()
        }

        fun refresh() {
            _uiState.update { it.copy(isLoading = true) }
            viewModelScope.launch {
                val stats = storageStatsRepository.computeStats()
                _uiState.update { it.copy(stats = stats, isLoading = false) }
            }
        }
    }
