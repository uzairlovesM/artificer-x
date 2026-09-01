package com.waheed.artificerx.data.repository

import com.waheed.artificerx.data.local.db.ProjectDao
import com.waheed.artificerx.data.local.db.ProjectEntity
import com.waheed.artificerx.data.local.db.ProjectVersionDao
import com.waheed.artificerx.data.local.db.ProjectVersionEntity
import com.waheed.artificerx.domain.model.CanvasLayer
import com.waheed.artificerx.domain.model.CanvasProjectState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Section 27 Version History + crash-safe save (Section 147) backing
 * store. StudioViewModel's flushToDisk() calls saveCurrentState() here
 * synchronously (blocking on Dispatchers.IO via runBlocking) from the
 * uncaught-exception handler path, since a crashing process cannot
 * await a coroutine that might not get scheduled before the JVM dies.
 */
@Singleton
class ProjectRepository
    @Inject
    constructor(
        private val projectDao: ProjectDao,
        private val versionDao: ProjectVersionDao,
    ) {
        private val json = Json { encodeDefaults = true }

        val allProjects: Flow<List<ProjectEntity>> = projectDao.observeAllProjects()

        suspend fun saveCurrentState(state: CanvasProjectState) {
            val existing = projectDao.getProjectById(state.projectId)
            val now = System.currentTimeMillis()
            val entity =
                ProjectEntity(
                    id = state.projectId,
                    name = state.projectName,
                    canvasWidthPx = state.canvasWidthPx,
                    canvasHeightPx = state.canvasHeightPx,
                    layersJson = json.encodeToString(state.layers),
                    activeLayerId = state.activeLayerId,
                    thumbnailPath = existing?.thumbnailPath,
                    createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                    lastModifiedEpochMillis = now,
                    lastOpenedEpochMillis = now,
                )
            projectDao.upsertProject(entity)
        }

        fun saveCurrentStateBlocking(state: CanvasProjectState) {
            kotlinx.coroutines.runBlocking {
                saveCurrentState(state)
            }
        }

        suspend fun createVersionCheckpoint(
            state: CanvasProjectState,
            triggeredBy: String,
            label: String,
        ) {
            versionDao.insertVersion(
                ProjectVersionEntity(
                    id = UUID.randomUUID().toString(),
                    projectId = state.projectId,
                    versionLabel = label,
                    layersJson = json.encodeToString(state.layers),
                    thumbnailPath = null,
                    triggeredBy = triggeredBy,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }

        suspend fun loadProject(projectId: String): CanvasProjectState? {
            val entity = projectDao.getProjectById(projectId) ?: return null
            val layers =
                runCatching {
                    json.decodeFromString<List<CanvasLayer>>(entity.layersJson)
                }.getOrDefault(emptyList())

            return CanvasProjectState(
                projectId = entity.id,
                projectName = entity.name,
                layers = layers,
                activeLayerId = entity.activeLayerId,
                canvasWidthPx = entity.canvasWidthPx,
                canvasHeightPx = entity.canvasHeightPx,
            )
        }

        fun observeVersionHistory(projectId: String): Flow<List<ProjectVersionEntity>> = versionDao.observeVersionsForProject(projectId)

        suspend fun deleteProject(projectId: String) {
            val entity = projectDao.getProjectById(projectId) ?: return
            versionDao.deleteAllVersionsForProject(projectId)
            projectDao.deleteProject(entity)
        }
    }
