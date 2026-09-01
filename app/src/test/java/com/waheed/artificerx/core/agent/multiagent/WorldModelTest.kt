package com.waheed.artificerx.core.agent.multiagent

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldModelTest {
    @Test
    fun `empty world model produces no prompt block`() {
        val worldModel = WorldModel(projectId = "p1")
        assertTrue(worldModel.isEmpty)
        assertNull(worldModel.toPromptBlock())
    }

    @Test
    fun `world model with only a style still produces a prompt block`() {
        val worldModel = WorldModel(projectId = "p1", establishedStyle = "watercolor, soft edges")
        assertTrue(worldModel.toPromptBlock()!!.contains("watercolor, soft edges"))
    }

    @Test
    fun `world model prompt block includes all established fields`() {
        val worldModel =
            WorldModel(
                projectId = "p1",
                establishedStyle = "cel-shaded anime",
                establishedPalette = "warm oranges and deep purples",
                establishedSetting = "a floating sky city at dusk",
                characters =
                    listOf(
                        EstablishedCharacter(name = "Mira", traits = "silver hair, blue cloak, star-shaped pendant"),
                    ),
                additionalNotes = "user prefers dynamic diagonal compositions",
            )
        val block = worldModel.toPromptBlock()!!
        assertTrue(block.contains("cel-shaded anime"))
        assertTrue(block.contains("warm oranges and deep purples"))
        assertTrue(block.contains("a floating sky city at dusk"))
        assertTrue(block.contains("Mira"))
        assertTrue(block.contains("silver hair, blue cloak, star-shaped pendant"))
        assertTrue(block.contains("dynamic diagonal compositions"))
    }

    @Test
    fun `world model with multiple characters lists each one`() {
        val worldModel =
            WorldModel(
                projectId = "p1",
                characters =
                    listOf(
                        EstablishedCharacter(name = "Mira", traits = "silver hair"),
                        EstablishedCharacter(name = "Rook", traits = "black feathers, one gold eye"),
                    ),
            )
        val block = worldModel.toPromptBlock()!!
        assertTrue(block.contains("Mira"))
        assertTrue(block.contains("Rook"))
        assertTrue(block.contains("black feathers, one gold eye"))
    }

    @Test
    fun `default lastUpdatedEpochMillis is recent`() {
        val before = System.currentTimeMillis()
        val worldModel = WorldModel(projectId = "p1")
        val after = System.currentTimeMillis()
        assertTrue(worldModel.lastUpdatedEpochMillis in before..after)
    }
}
