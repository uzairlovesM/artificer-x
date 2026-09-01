package com.waheed.artificerx.core.agent.multiagent

import com.waheed.artificerx.data.local.db.WorldModelDao
import com.waheed.artificerx.data.local.db.WorldModelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Minimal in-memory fake — WorldModelDao is a plain interface, so no
 *  Room/Android runtime is needed to exercise WorldModelStore's merge
 *  logic in a fast JVM unit test. */
private class FakeWorldModelDao : WorldModelDao {
    val rows = mutableMapOf<String, WorldModelEntity>()

    override suspend fun getByProjectId(projectId: String): WorldModelEntity? = rows[projectId]

    override fun observeByProjectId(projectId: String): Flow<WorldModelEntity?> = MutableStateFlow(rows[projectId])

    override suspend fun upsert(entity: WorldModelEntity) {
        rows[entity.projectId] = entity
    }

    override suspend fun deleteByProjectId(projectId: String) {
        rows.remove(projectId)
    }
}

class WorldModelStoreTest {
    @Test
    fun `get on a project with no saved world model returns an empty one`() =
        runBlocking {
            val store = WorldModelStore(FakeWorldModelDao())
            val result = store.get("new-project")
            assertTrue(result.isEmpty)
            assertEquals("new-project", result.projectId)
        }

    @Test
    fun `mergeUpdate does not overwrite fields it wasn't given new values for`() =
        runBlocking {
            val store = WorldModelStore(FakeWorldModelDao())
            store.mergeUpdate(
                projectId = "p1",
                establishedStyle = "watercolor",
                establishedPalette = "pastel",
            )
            // A later Archivist pass only reports a new setting — style and
            // palette must survive, not get wiped to null.
            store.mergeUpdate(projectId = "p1", establishedSetting = "a quiet forest clearing")

            val result = store.get("p1")
            assertEquals("watercolor", result.establishedStyle)
            assertEquals("pastel", result.establishedPalette)
            assertEquals("a quiet forest clearing", result.establishedSetting)
        }

    @Test
    fun `mergeUpdate replaces an existing character's traits by matching name case-insensitively`() =
        runBlocking {
            val store = WorldModelStore(FakeWorldModelDao())
            store.mergeUpdate(
                projectId = "p1",
                newOrUpdatedCharacters = listOf(EstablishedCharacter("Mira", "silver hair, blue cloak")),
            )
            store.mergeUpdate(
                projectId = "p1",
                newOrUpdatedCharacters = listOf(EstablishedCharacter("mira", "silver hair, red cloak, new scar")),
            )

            val result = store.get("p1")
            assertEquals(1, result.characters.size)
            assertTrue(
                result.characters
                    .first()
                    .traits
                    .contains("new scar"),
            )
        }

    @Test
    fun `mergeUpdate appends a new character without touching existing ones`() =
        runBlocking {
            val store = WorldModelStore(FakeWorldModelDao())
            store.mergeUpdate(projectId = "p1", newOrUpdatedCharacters = listOf(EstablishedCharacter("Mira", "silver hair")))
            store.mergeUpdate(projectId = "p1", newOrUpdatedCharacters = listOf(EstablishedCharacter("Rook", "black feathers")))

            val result = store.get("p1")
            assertEquals(2, result.characters.size)
            assertTrue(result.characters.any { it.name == "Mira" })
            assertTrue(result.characters.any { it.name == "Rook" })
        }

    @Test
    fun `clear removes the stored world model for a project`() =
        runBlocking {
            val store = WorldModelStore(FakeWorldModelDao())
            store.mergeUpdate(projectId = "p1", establishedStyle = "watercolor")
            store.clear("p1")

            val result = store.get("p1")
            assertTrue(result.isEmpty)
        }
}
