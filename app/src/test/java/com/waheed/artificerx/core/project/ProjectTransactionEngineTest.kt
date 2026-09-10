package com.waheed.artificerx.core.project

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProjectTransactionEngineTest {
    private fun snapshot() = ProjectSnapshot(
        metadata = ProjectMetadata(ProjectId("p1"), "Demo", 1024, 1024, 1L, 1L),
        layers = listOf(LayerDescriptor(LayerId("base"), "Background")),
        activeLayer = LayerId("base"),
        saved = true,
    )

    @Test
    fun mutationIsAtomicAndRevisionAdvances() {
        val engine = ProjectTransactionEngine(snapshot())
        val result = engine.mutate("rename") { rename("Renamed") }
        assertTrue(result.success)
        assertEquals("Renamed", engine.current().metadata.title)
        assertEquals(1L, engine.current().metadata.revision)
        assertTrue(engine.isDirty())
    }

    @Test
    fun failedMutationDoesNotChangeSnapshot() {
        val engine = ProjectTransactionEngine(snapshot())
        val before = engine.current()
        val result = engine.mutate("remove-missing") { removeLayer(LayerId("missing")) }
        assertTrue(!result.success)
        assertEquals(before, engine.current())
    }
}
