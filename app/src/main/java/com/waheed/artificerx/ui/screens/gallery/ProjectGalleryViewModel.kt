package com.waheed.artificerx.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.local.db.ProjectEntity
import com.waheed.artificerx.data.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
        val projects: StateFlow<List<ProjectEntity>> =
            projectRepository.allProjects.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

        fun deleteProject(projectId: String) {
            viewModelScope.launch {
                projectRepository.deleteProject(projectId)
            }
        }
    }
