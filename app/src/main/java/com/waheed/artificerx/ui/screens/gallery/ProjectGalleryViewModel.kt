package com.waheed.artificerx.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.local.db.ProjectEntity
import com.waheed.artificerx.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Section 27 Version History / project management screen backing
 * state. Lists every saved project (StudioViewModel's crash-safe
 * flush and manual saveNow() both write here), newest-modified first.
 */
@HiltViewModel
class ProjectGalleryViewModel
    @Inject
    constructor(
        private val projectRepository: ProjectRepository,
    ) : ViewModel() {
        private val _lastError = MutableStateFlow<String?>(null)
        val lastError = _lastError.asStateFlow()

        val projects: StateFlow<List<ProjectEntity>> =
            projectRepository.allProjects.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        fun deleteProject(projectId: String) {
            viewModelScope.launch {
                _lastError.value = null
                runCatching { projectRepository.deleteProject(projectId) }
                    .onFailure { _lastError.value = it.message ?: "Could not delete project." }
            }
        }

        fun dismissError() {
            _lastError.value = null
        }
    }
