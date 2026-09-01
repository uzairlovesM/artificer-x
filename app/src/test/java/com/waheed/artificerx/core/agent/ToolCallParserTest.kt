package com.waheed.artificerx.core.agent

import com.waheed.artificerx.core.network.FunctionCallDto
import com.waheed.artificerx.core.network.ToolCallDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * ToolCallParser is the boundary between an untrusted LLM's raw JSON
 * tool_call arguments and the app's typed domain. Its own KDoc promises
 * every field access is defensive and never crashes on malformed input
 * (Section 191 Reliability Engineering) — these tests hold that promise
 * to actual assertions instead of leaving it as an unverified comment.
 */
class ToolCallParserTest {
    private fun toolCall(
        name: String,
        argsJson: String,
        id: String = "call_1",
    ) = ToolCallDto(id = id, function = FunctionCallDto(name = name, arguments = argsJson))

    @Test
    fun `create_layer with valid name parses correctly`() {
        val result = ToolCallParser.parse(toolCall("create_layer", """{"name":"Character Outline"}"""))
        assertTrue(result is ParsedToolCall.CreateLayer)
        assertEquals("Character Outline", (result as ParsedToolCall.CreateLayer).name)
    }

    @Test
    fun `create_layer with missing name falls back to default instead of crashing`() {
        val result = ToolCallParser.parse(toolCall("create_layer", "{}"))
        assertTrue(result is ParsedToolCall.CreateLayer)
        assertEquals("New Layer", (result as ParsedToolCall.CreateLayer).name)
    }

    @Test
    fun `draw_path with valid points array parses all coordinates in order`() {
        val result =
            ToolCallParser.parse(
                toolCall("draw_path", """{"points":[10,20,30,40],"color_hex":"#FF0000"}"""),
            )
        assertTrue(result is ParsedToolCall.DrawPath)
        val drawPath = result as ParsedToolCall.DrawPath
        assertEquals(listOf(10f, 20f, 30f, 40f), drawPath.points)
        assertEquals("#FF0000", drawPath.colorHex)
    }

    @Test
    fun `draw_path with completely malformed JSON does not throw and returns empty points`() {
        val result = ToolCallParser.parse(toolCall("draw_path", "not valid json at all {{{"))
        assertTrue(result is ParsedToolCall.DrawPath)
        assertEquals(emptyList<Float>(), (result as ParsedToolCall.DrawPath).points)
    }

    @Test
    fun `draw_path with non-numeric entries in points array drops them instead of crashing`() {
        val result = ToolCallParser.parse(toolCall("draw_path", """{"points":[10,"not_a_number",30,40]}"""))
        assertTrue(result is ParsedToolCall.DrawPath)
        // Non-numeric entries are filtered rather than causing a parse failure.
        assertEquals(listOf(10f, 30f, 40f), (result as ParsedToolCall.DrawPath).points)
    }

    @Test
    fun `finish_turn parses summary field`() {
        val result = ToolCallParser.parse(toolCall("finish_turn", """{"summary":"Drew a red circle"}"""))
        assertTrue(result is ParsedToolCall.FinishTurn)
        assertEquals("Drew a red circle", (result as ParsedToolCall.FinishTurn).summary)
    }

    @Test
    fun `unknown tool name does not throw`() {
        // An unrecognized function name must degrade gracefully (Unknown/
        // fallback) rather than propagate an exception up through the
        // agent loop and crash the whole turn.
        val result = runCatching { ToolCallParser.parse(toolCall("nonexistent_tool_xyz", "{}")) }
        assertTrue(result.isSuccess)
    }

    @Test
    fun `empty arguments string does not throw for any known tool`() {
        val toolNames =
            listOf(
                "create_layer",
                "delete_layer",
                "set_active_layer",
                "draw_path",
                "draw_shape",
                "apply_gradient",
                "fill_region",
                "set_layer_property",
                "duplicate_layer",
                "flip_layer",
                "crop_canvas",
                "inspect_canvas",
                "pick_color",
                "apply_filter",
                "add_text",
                "create_mask",
                "enable_symmetry",
                "apply_pattern",
                "draw_curve",
                "import_image_layer",
                "create_primitive",
                "sculpt_stroke",
                "delete_mesh",
                "set_mesh_color",
                "transform_mesh",
                "inspect_scene",
                "finish_turn",
            )
        toolNames.forEach { name ->
            val result = runCatching { ToolCallParser.parse(toolCall(name, "")) }
            assertTrue("Tool '$name' threw on empty arguments", result.isSuccess)
        }
    }
}
