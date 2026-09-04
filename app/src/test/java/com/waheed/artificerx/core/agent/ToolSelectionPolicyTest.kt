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
        // Previously asserted > 3_000 — that counted synthetic numbered
        // placeholder tool schemas that have since been removed (see
        // ToolRegistry's "reliability audit" note): unaudited generated
        // stubs whose operations didn't map to real executors. The
        // catalog is now the ~55 real BUILTIN_TOOLS plus the 2
        // invoke/search meta-tools that expose the 1000+ *recipe*
        // catalog as runtime data instead of 1000+ individual schemas
        // (RuntimeToolCatalog contributes 0 here since it needs an
        // Android Context this unit test doesn't have). The genuinely
        // testable invariant is that the catalog stays comfortably
        // above what a single hand-written tool list would be, i.e.
        // it's actually built from BUILTIN_TOOLS + recipe tools, not
        // that it hits a specific historical count.
        assertTrue(ToolRegistry.ALL_TOOLS.size > 40)
    }
}
