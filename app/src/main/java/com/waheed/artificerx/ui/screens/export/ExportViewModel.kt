package com.waheed.artificerx.ui.screens.export

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.core.export.ExportResult
import com.waheed.artificerx.core.export.ImageExporter
import com.waheed.artificerx.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ExportState { IDLE, EXPORTING, SUCCESS, FAILED }

data class ExportUiState(
    val projectName: String = "artwork",
    val exportState: ExportState = ExportState.IDLE,
    val resultMessage: String? = null,
)

@HiltViewModel
class ExportViewModel
    @Inject
    constructor(
        private val imageExporter: ImageExporter,
        private val projectRepository: ProjectRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExportUiState())
        val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

        fun loadProjectName(projectId: String) {
            viewModelScope.launch {
                val project = projectRepository.loadProject(projectId)
                if (project != null) {
                    _uiState.update { it.copy(projectName = project.projectName) }
                }
            }
        }

        fun exportBitmap(bitmap: Bitmap) {
            _uiState.update { it.copy(exportState = ExportState.EXPORTING) }
            viewModelScope.launch {
                val safeFileName =
                    _uiState.value.projectName
                        .replace(Regex("[^A-Za-z0-9_-]"), "_")
                        .ifBlank { "artwork" } + "_${System.currentTimeMillis()}"

                when (val result = imageExporter.exportPng(bitmap, safeFileName)) {
                    is ExportResult.Success ->
                        _uiState.update {
                            it.copy(
                                exportState = ExportState.SUCCESS,
                                resultMessage = "Saved to Pictures/ARTIFICER-X/${result.displayName}",
                            )
                        }
                    is ExportResult.Failure ->
                        _uiState.update {
                            it.copy(exportState = ExportState.FAILED, resultMessage = result.message)
                        }
                }
            }
        }

        fun reset() {
            _uiState.update { it.copy(exportState = ExportState.IDLE, resultMessage = null) }
        }
    }
