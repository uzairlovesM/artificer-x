package com.waheed.artificerx.drawing

import com.waheed.artificerx.data.local.db.ProjectDao
import com.waheed.artificerx.data.repository.ProjectRepository as PersistentProjectRepository
import com.waheed.artificerx.domain.Project
import com.waheed.artificerx.domain.toDomain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Compatibility facade for legacy drawing callers. The canonical persistence
 * implementation remains data.repository.ProjectRepository. */
@Singleton
class ProjectRepository @Inject constructor(
    private val dao: ProjectDao,
    private val persistent: PersistentProjectRepository,
) {
    fun getRecentProjects(): Flow<List<Project>> = dao.observeAllProjects().map { rows -> rows.map { it.toDomain() } }

    suspend fun saveProject(project: Project): Long {
        dao.upsertProject(project.toEntity())
        return 1L
    }

    suspend fun updateProject(project: Project): Int {
        dao.updateProject(project.toEntity())
        return 1
    }

    suspend fun deleteProject(project: Project): Int {
        dao.deleteProject(project.toEntity())
        return 1
    }

    suspend fun addArtifacts(projectId: String, newArtifacts: List<String>) {
        val current = dao.getProjectById(projectId) ?: return
        val nextThumbnail = newArtifacts.lastOrNull() ?: current.thumbnailPath
        dao.updateProject(current.copy(thumbnailPath = nextThumbnail, lastModifiedEpochMillis = System.currentTimeMillis()))
    }

    fun getProjectCount(): Int = runBlocking { dao.getProjectCount() }

    fun newProject(name: String): Project = Project(projectId = UUID.randomUUID().toString(), name = name)
}
