package com.waheed.artificerx.core.agent.multiagent

import org.junit.Assert.assertEquals
import org.junit.Test

class AgentPlannerTest {
    private val planner = AgentPlanner()

    @Test
    fun `generic request with no signal words defaults to MASTER`() {
        assertEquals(AgentRole.MASTER, planner.selectRole("draw a cat sitting on a fence", is3DMode = false))
    }

    @Test
    fun `character keyword selects CHARACTER role`() {
        assertEquals(AgentRole.CHARACTER, planner.selectRole("keep the same character consistent across this scene", is3DMode = false))
    }

    @Test
    fun `lighting keyword selects LIGHTING role`() {
        assertEquals(AgentRole.LIGHTING, planner.selectRole("make the shadow softer on the left side", is3DMode = false))
    }

    @Test
    fun `composition keyword selects COMPOSITION role`() {
        assertEquals(AgentRole.COMPOSITION, planner.selectRole("improve the layout and framing of this piece", is3DMode = false))
    }

    @Test
    fun `environment keyword selects ENVIRONMENT role`() {
        assertEquals(AgentRole.ENVIRONMENT, planner.selectRole("add a forest background", is3DMode = false))
    }

    @Test
    fun `style keyword selects ART_DIRECTOR role`() {
        assertEquals(AgentRole.ART_DIRECTOR, planner.selectRole("change the overall mood and color scheme", is3DMode = false))
    }

    @Test
    fun `3D mode with no other signal defaults to RENDERING`() {
        assertEquals(AgentRole.RENDERING, planner.selectRole("add more detail to the model", is3DMode = true))
    }

    @Test
    fun `signal detection is case-insensitive`() {
        assertEquals(AgentRole.LIGHTING, planner.selectRole("ADD MORE SHADOW HERE", is3DMode = false))
    }

    @Test
    fun `character signal takes priority over 3D default`() {
        // A character-consistency request in 3D mode should still route
        // to CHARACTER rather than falling through to RENDERING, since
        // character continuity is the more specific and more important
        // signal here.
        assertEquals(AgentRole.CHARACTER, planner.selectRole("keep the same character look", is3DMode = true))
    }
}
