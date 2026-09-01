package com.waheed.artificerx.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectVersionDao {
    @Query("SELECT * FROM project_versions WHERE projectId = :projectId ORDER BY createdAtEpochMillis DESC")
    fun observeVersionsForProject(projectId: String): Flow<List<ProjectVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: ProjectVersionEntity)

    @Query("SELECT * FROM project_versions WHERE id = :versionId")
    suspend fun getVersionById(versionId: String): ProjectVersionEntity?

    @Query("DELETE FROM project_versions WHERE projectId = :projectId")
    suspend fun deleteAllVersionsForProject(projectId: String)

    @Query("SELECT COUNT(*) FROM project_versions WHERE projectId = :projectId")
    suspend fun getVersionCountForProject(projectId: String): Int
}
