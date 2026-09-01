package com.waheed.artificerx.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.local.db.ProjectVersionEntity
import com.waheed.artificerx.data.repository.ProjectRepository
import com.waheed.artificerx.domain.model.CanvasLayer
import com.waheed.artificerx.domain.model.CanvasProjectState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed class RestoreOutcome {
    data class Success(
        val restoredState: CanvasProjectState,
    ) : RestoreOutcome()

    data class Failure(
        val message: String,
    ) : RestoreOutcome()
}

data class VersionHistoryUiState(
    val restoreOutcome: RestoreOutcome? = null,
    val pendingRestoreVersionId: String? = null,
)

/**
 * Section 27 Version History browsing + restore. Restoring a version
 * does NOT delete anything newer — it loads that checkpoint's layer
 * state back into the live project, and the next auto-save/checkpoint
 * captures the restored state as the new "current," preserving the
 * full timeline rather than truncating it (a destructive restore would
 * violate the "never lose work" spirit of the whole crash-safe-save
 * design in Section 147).
 */
@HiltViewModel
class VersionHistoryViewModel
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
    ) : ViewModel() {
        private val json = Json { ignoreUnknownKeys = true }
        private val _uiState = MutableStateFlow(VersionHistoryUiState())
        val uiState: StateFlow<VersionHistoryUiState> = _uiState.asStateFlow()

        private var currentProjectId: String? = null

        fun versionsFor(projectId: String): StateFlow<List<ProjectVersionEntity>> {
            currentProjectId = projectId
            return projectRepository.observeVersionHistory(projectId).stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )
        }

        fun requestRestore(versionId: String) {
            _uiState.update { it.copy(pendingRestoreVersionId = versionId) }
        }

        fun cancelRestoreConfirmation() {
            _uiState.update { it.copy(pendingRestoreVersionId = null) }
        }

        fun confirmRestore(version: ProjectVersionEntity) {
            viewModelScope.launch {
                val layers =
                    runCatching {
                        json.decodeFromString<List<CanvasLayer>>(version.layersJson)
                    }.getOrNull()

                if (layers == null) {
                    _uiState.update {
                        it.copy(
                            restoreOutcome = RestoreOutcome.Failure("This checkpoint's data could not be read."),
                            pendingRestoreVersionId = null,
                        )
                    }
                    return@launch
                }

                val existingProject = projectRepository.loadProject(version.projectId)
                val restoredState =
                    CanvasProjectState(
                        projectId = version.projectId,
                        projectName = existingProject?.projectName ?: "Untitled",
                        layers = layers,
                        activeLayerId = layers.firstOrNull()?.id,
                        canvasWidthPx = existingProject?.canvasWidthPx ?: 1024,
                        canvasHeightPx = existingProject?.canvasHeightPx ?: 1024,
                    )

                projectRepository.saveCurrentState(restoredState)
                projectRepository.createVersionCheckpoint(
                    restoredState,
                    triggeredBy = "restore",
                    label = "Restored from '${version.versionLabel}'",
                )

                _uiState.update {
                    it.copy(
                        restoreOutcome = RestoreOutcome.Success(restoredState),
                        pendingRestoreVersionId = null,
                    )
                }
            }
        }

        fun clearOutcome() {
            _uiState.update { it.copy(restoreOutcome = null) }
        }
    }
