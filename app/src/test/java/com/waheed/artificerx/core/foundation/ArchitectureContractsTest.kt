package com.waheed.artificerx.core.foundation

import com.waheed.artificerx.core.architecture.ArchitectureRules
import com.waheed.artificerx.core.architecture.RuntimeBoundary
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ArchitectureContractsTest {
    @Test
    fun drawingCannotDependOnNetworkBoundary() {
        assertFalse(ArchitectureRules.isAllowed(RuntimeBoundary.DRAWING, RuntimeBoundary.RESEARCH))
        assertTrue(ArchitectureRules.isAllowed(RuntimeBoundary.AI, RuntimeBoundary.TOOL))
    }
}
