package com.waheed.artificerx.core.project
import org.junit.Assert.*
import org.junit.Test

class ProjectValidatorTest {
    @Test fun detectsDuplicateLayerIds() {
        val p = ProjectSnapshot(
            ProjectMetadata(ProjectId("p"),"Demo",100,100,1,1),
            listOf(
                LayerDescriptor(LayerId("x"),"A"),
                LayerDescriptor(LayerId("x"),"B")
            ), null, true
        )
        assertFalse(ProjectValidator().isSafe(p))
    }
}
