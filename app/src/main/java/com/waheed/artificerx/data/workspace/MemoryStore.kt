package com.waheed.artificerx.data.workspace

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val threadId: String?,
    val namespace: String,
    val key: String,
    val value: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE namespace = :namespace ORDER BY updatedAtEpochMillis DESC")
    suspend fun list(namespace: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE namespace = :namespace AND (key LIKE '%' || :query || '%' OR value LIKE '%' || :query || '%') ORDER BY updatedAtEpochMillis DESC LIMIT 20")
    suspend fun search(namespace: String, query: String): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE namespace = :namespace AND key = :key")
    suspend fun delete(namespace: String, key: String)
}

@javax.inject.Singleton
class MemoryRepository @javax.inject.Inject constructor(private val dao: MemoryDao) {
    suspend fun remember(namespace: String, key: String, value: String, threadId: String? = null) {
        val now = System.currentTimeMillis()
        val safeNamespace = namespace.trim().lowercase().take(80).ifBlank { "global" }
        val safeKey = key.trim().lowercase().take(100)
        val id = "$safeNamespace:$safeKey"
        val existing = dao.list(safeNamespace).firstOrNull { it.key == safeKey }
        dao.upsert(MemoryEntity(id, threadId ?: existing?.threadId, safeNamespace, safeKey, value.trim().take(5000), existing?.createdAtEpochMillis ?: now, now))
    }
    suspend fun recall(namespace: String, query: String): List<MemoryEntity> = dao.search(namespace.trim().lowercase().take(80).ifBlank { "global" }, query.trim().take(200))
    suspend fun list(namespace: String = "global"): List<MemoryEntity> = dao.list(namespace.trim().lowercase().take(80).ifBlank { "global" })
    suspend fun delete(namespace: String, key: String) = dao.delete(namespace.trim().lowercase().take(80).ifBlank { "global" }, key.trim().lowercase().take(100))
}
