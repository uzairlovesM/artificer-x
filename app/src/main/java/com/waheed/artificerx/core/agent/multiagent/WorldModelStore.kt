package com.waheed.artificerx.core.agent.multiagent

import com.waheed.artificerx.data.local.db.WorldModelDao
import com.waheed.artificerx.data.local.db.WorldModelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads/saves the per-project [WorldModel] used to give every
 * [AgentRole] cross-turn continuity. Read on every
 * AgentOrchestrator.handleUserMessage() call (cheap — one Room row
 * lookup) and written whenever the [AgentRole.ARCHIVIST] role produces
 * an updated summary.
 */
@Singleton
class WorldModelStore
    @Inject
    constructor(
        private val dao: WorldModelDao,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        suspend fun get(projectId: String): WorldModel {
            val entity = dao.getByProjectId(projectId) ?: return WorldModel(projectId = projectId)
            return entity.toDomain()
        }

        fun observe(projectId: String): Flow<WorldModel> =
            dao.observeByProjectId(projectId).map { it?.toDomain() ?: WorldModel(projectId = projectId) }

        suspend fun save(worldModel: WorldModel) {
            dao.upsert(worldModel.toEntity())
        }

        suspend fun clear(projectId: String) {
            dao.deleteByProjectId(projectId)
        }

        /** Merges a partial Archivist update into the existing World Model
         *  rather than replacing it wholesale — a single turn's Archivist
         *  summary is rarely a complete restatement of everything ever
         *  established, and blindly overwriting would silently drop
         *  characters/style/setting the Archivist didn't happen to mention
         *  this time. Null fields in [update] mean "no new information",
         *  not "clear this field"; characters are merged by name (an
         *  update to an existing character's traits replaces that
         *  character's entry, a new name is appended). */
        suspend fun mergeUpdate(
            projectId: String,
            establishedStyle: String? = null,
            establishedPalette: String? = null,
            establishedSetting: String? = null,
            newOrUpdatedCharacters: List<EstablishedCharacter> = emptyList(),
            additionalNotes: String? = null,
        ) {
            val current = get(projectId)
            val mergedCharacters =
                current.characters
                    .filterNot { existing -> newOrUpdatedCharacters.any { it.name.equals(existing.name, ignoreCase = true) } } +
                    newOrUpdatedCharacters
            val merged =
                current.copy(
                    establishedStyle = establishedStyle ?: current.establishedStyle,
                    establishedPalette = establishedPalette ?: current.establishedPalette,
                    establishedSetting = establishedSetting ?: current.establishedSetting,
                    characters = mergedCharacters,
                    additionalNotes = additionalNotes ?: current.additionalNotes,
                    lastUpdatedEpochMillis = System.currentTimeMillis(),
                )
            save(merged)
        }

        private fun WorldModelEntity.toDomain(): WorldModel {
            val characters =
                runCatching {
                    json.decodeFromString<List<EstablishedCharacter>>(charactersJson)
                }.getOrDefault(emptyList())
            return WorldModel(
                projectId = projectId,
                establishedStyle = establishedStyle,
                establishedPalette = establishedPalette,
                establishedSetting = establishedSetting,
                characters = characters,
                additionalNotes = additionalNotes,
                lastUpdatedEpochMillis = lastUpdatedEpochMillis,
            )
        }

        private fun WorldModel.toEntity(): WorldModelEntity =
            WorldModelEntity(
                projectId = projectId,
                establishedStyle = establishedStyle,
                establishedPalette = establishedPalette,
                establishedSetting = establishedSetting,
                charactersJson = json.encodeToString(characters),
                additionalNotes = additionalNotes,
                lastUpdatedEpochMillis = lastUpdatedEpochMillis,
            )
    }
