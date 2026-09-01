package com.waheed.artificerx.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.repository.BackupRestoreRepository
import com.waheed.artificerx.data.repository.BackupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class BackupUiState(
    val availableBackups: List<File> = emptyList(),
    val isWorking: Boolean = false,
    val lastResultMessage: String? = null,
    val lastResultIsError: Boolean = false,
)

@HiltViewModel
class BackupRestoreViewModel
    @Inject
    constructor(
        private val backupRestoreRepository: BackupRestoreRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BackupUiState())
        val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

        init {
            refreshBackupList()
        }

        fun refreshBackupList() {
            _uiState.update { it.copy(availableBackups = backupRestoreRepository.listAvailableBackupFiles()) }
        }

        fun exportNow() {
            _uiState.update { it.copy(isWorking = true) }
            viewModelScope.launch {
                when (val result = backupRestoreRepository.exportAllToFile()) {
                    is BackupResult.ExportSuccess -> {
                        _uiState.update {
                            it.copy(
                                isWorking = false,
                                lastResultMessage = "Exported ${result.projectCount} project(s) to ${result.filePath}",
                                lastResultIsError = false,
                                availableBackups = backupRestoreRepository.listAvailableBackupFiles(),
                            )
                        }
                    }
                    is BackupResult.Failure ->
                        _uiState.update {
                            it.copy(isWorking = false, lastResultMessage = result.message, lastResultIsError = true)
                        }
                    else -> Unit
                }
            }
        }

        fun restoreFrom(file: File) {
            _uiState.update { it.copy(isWorking = true) }
            viewModelScope.launch {
                when (val result = backupRestoreRepository.importFromFile(file.absolutePath)) {
                    is BackupResult.ImportSuccess ->
                        _uiState.update {
                            it.copy(
                                isWorking = false,
                                lastResultMessage = "Restored ${result.projectCount} project(s) and ${result.versionCount} checkpoint(s)",
                                lastResultIsError = false,
                            )
                        }
                    is BackupResult.Failure ->
                        _uiState.update {
                            it.copy(isWorking = false, lastResultMessage = result.message, lastResultIsError = true)
                        }
                    else -> Unit
                }
            }
        }

        fun dismissMessage() {
            _uiState.update { it.copy(lastResultMessage = null) }
        }
    }
