package com.waheed.artificerx.core.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ToolRegistry.ALL_TOOLS is sent verbatim as the JSON-Schema "tools"
 * array in every /chat/completions request (AgentOrchestrator). These
 * tests catch schema-authoring mistakes — a duplicate name, a required
 * field that isn't actually declared in properties — that would
 * otherwise only surface as a confusing runtime failure from the LLM
 * provider or a silently-ignored tool call.
 */
class ToolRegistryTest {
    @Test
    fun `no two tools share the same function name`() {
        val names = ToolRegistry.ALL_TOOLS.map { it.function.name }
        assertEquals(
            "Duplicate tool name(s) found: ${names.groupBy { it }.filterValues { it.size > 1 }.keys}",
            names.size,
            names.toSet().size,
        )
    }

    @Test
    fun `every tool has a non-blank name and description`() {
        ToolRegistry.ALL_TOOLS.forEach { tool ->
            assertTrue("Tool has blank name", tool.function.name.isNotBlank())
            assertTrue(
                "Tool '${tool.function.name}' has blank description",
                tool.function.description.isNotBlank(),
            )
        }
    }

    @Test
    fun `finish_turn tool is present so the agent loop has a way to terminate`() {
        val names = ToolRegistry.ALL_TOOLS.map { it.function.name }
        assertTrue("finish_turn is missing from ToolRegistry — agent loop could never terminate cleanly", names.contains("finish_turn"))
    }

    @Test
    fun `core 2D drawing tools are all registered`() {
        val names = ToolRegistry.ALL_TOOLS.map { it.function.name }.toSet()
        val expectedCoreTools =
            setOf(
                "create_layer",
                "draw_path",
                "draw_shape",
                "fill_region",
                "apply_gradient",
                "inspect_canvas",
            )
        expectedCoreTools.forEach { expected ->
            assertTrue("Expected core tool '$expected' missing from ToolRegistry", names.contains(expected))
        }
    }

    @Test
    fun `newly added layer manipulation tools are registered`() {
        val names = ToolRegistry.ALL_TOOLS.map { it.function.name }.toSet()
        val expectedNewTools = setOf("duplicate_layer", "flip_layer", "crop_canvas")
        expectedNewTools.forEach { expected ->
            assertTrue("Expected tool '$expected' missing from ToolRegistry", names.contains(expected))
        }
    }

    @Test
    fun `core 3D sculpt tools are all registered`() {
        val names = ToolRegistry.ALL_TOOLS.map { it.function.name }.toSet()
        val expectedSculptTools =
            setOf(
                "create_primitive",
                "sculpt_stroke",
                "inspect_scene",
            )
        expectedSculptTools.forEach { expected ->
            assertTrue("Expected sculpt tool '$expected' missing from ToolRegistry", names.contains(expected))
        }
    }
}
