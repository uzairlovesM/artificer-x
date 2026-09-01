package com.waheed.artificerx.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorldModelDao {
    @Query("SELECT * FROM world_models WHERE projectId = :projectId")
    suspend fun getByProjectId(projectId: String): WorldModelEntity?

    @Query("SELECT * FROM world_models WHERE projectId = :projectId")
    fun observeByProjectId(projectId: String): Flow<WorldModelEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WorldModelEntity)

    @Query("DELETE FROM world_models WHERE projectId = :projectId")
    suspend fun deleteByProjectId(projectId: String)
}
