package com.waheed.artificerx.core.agent

import org.junit.Assert.assertTrue
import org.junit.Test

class ToolSelectionPolicyTest {
    @Test
    fun visualPromptKeepsCoreArtifactAndVisualTools() {
        val names = ToolSelectionPolicy.select("draw an anime poster and export it as a PNG", 60).map { it.function.name }.toSet()
        assertTrue("draw_path" in names)
        assertTrue("generate_image" in names)
        assertTrue("create_file" in names)
        assertTrue("create_zip" in names)
    }

    @Test
    fun massiveCatalogIsNotSentWholeToProvider() {
        assertTrue(ToolSelectionPolicy.select("fix my Kotlin project", 180).size <= 180)
        assertTrue(ToolRegistry.ALL_TOOLS.size > 3_000)
    }
}
