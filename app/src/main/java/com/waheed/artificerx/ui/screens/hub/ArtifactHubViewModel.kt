package com.waheed.artificerx.ui.screens.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.repository.ChatWorkspaceRepository
import com.waheed.artificerx.data.workspace.ArtifactEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArtifactHubViewModel @Inject constructor(
    private val repository: ChatWorkspaceRepository,
) : ViewModel() {
    val artifacts: StateFlow<List<ArtifactEntity>> = repository.observeAllArtifacts().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteArtifact(id) }
    }
}
