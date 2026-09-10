package com.waheed.artificerx.drawing

import androidx.lifecycle.LiveData
import com.waheed.artificerx.data.local.ProjectDao
import com.waheed.artificerx.domain.Project
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao
) {
    suspend fun saveProject(project: Project): Long = projectDao.insert(project)
    suspend fun updateProject(project: Project): Int = projectDao.update(project)
    suspend fun deleteProject(project: Project): Int = projectDao.delete(project)

    fun getRecentProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun addArtifacts(projectId: String, newArtifacts: List<Bitmap>) {
        val project = projectDao.getProjectById(projectId)
        project?.let {
            val allArtifacts = if (it.artifacts.isEmpty()) newArtifacts else it.artifacts + newArtifacts
            projectDao.refreshArtifacts(it, ArrayList(allArtifacts))
        }
    }

    fun getProjectCount(): Int = projectDao.getProjectCount()
}