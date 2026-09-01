package com.waheed.artificerx.core.mesh

import com.waheed.artificerx.domain.model.SculptMesh
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thread-safe holder for the active 3D sculpt scene's mesh list —
 * the 3D counterpart to LayerBitmapStore. Meshes are immutable data
 * classes (Section MeshModels), so every sculpt-brush stroke or
 * primitive-add operation replaces a mesh entry rather than mutating
 * fields in place, which keeps this store trivially safe to read from
 * the render thread while writes happen from the agent's coroutine.
 */
@Singleton
class SculptSceneStore
    @Inject
    constructor() {
        private val _meshes = MutableStateFlow<Map<String, SculptMesh>>(emptyMap())
        val meshes: StateFlow<Map<String, SculptMesh>> = _meshes.asStateFlow()

        /** Monotonically incremented on every mesh mutation so the Filament
         *  renderer knows to re-upload vertex buffers instead of polling
         *  deep-equality on potentially tens of thousands of vertices every
         *  frame — a plain revision counter is far cheaper. */
        private val _revision = MutableStateFlow(0L)
        val revision: StateFlow<Long> = _revision.asStateFlow()

        fun addMesh(mesh: SculptMesh) {
            _meshes.update { it + (mesh.id to mesh) }
            bumpRevision()
        }

        fun updateMesh(mesh: SculptMesh) {
            _meshes.update { current ->
                if (!current.containsKey(mesh.id)) return@update current
                current + (mesh.id to mesh)
            }
            bumpRevision()
        }

        fun removeMesh(meshId: String) {
            _meshes.update { it - meshId }
            bumpRevision()
        }

        fun getMesh(meshId: String): SculptMesh? = _meshes.value[meshId]

        fun clearAll() {
            _meshes.update { emptyMap() }
            bumpRevision()
        }

        private fun bumpRevision() {
            _revision.update { it + 1 }
        }
    }
