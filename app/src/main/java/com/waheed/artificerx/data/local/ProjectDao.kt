package com.waheed.artificerx.data.local

import androidx.room.*
import com.waheed.artificerx.domain.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project): Int

    @Delete
    suspend fun delete(project: Project): Int

    @Query("SELECT * FROM projects WHERE projectId = :id")
    suspend fun getProjectById(id: String): Project?

    @Query("SELECT * FROM projects ORDER BY lastModified DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT COUNT(*) FROM projects")
    suspend fun getProjectCount(): Int

    @Transaction
    fun refreshArtifacts(project: Project, newArtifacts: List<Bitmap>) {
        project.artifacts = ArrayList(newArtifacts)
        update(project)
    }
}