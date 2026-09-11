package com.waheed.artificerx.core.foundation

import com.waheed.artificerx.core.architecture.ArchitectureRules
import com.waheed.artificerx.core.architecture.RuntimeBoundary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureContractsTest {
    @Test
    fun drawingCannotDependOnNetworkBoundary() {
        assertFalse(ArchitectureRules.isAllowed(RuntimeBoundary.DRAWING, RuntimeBoundary.RESEARCH))
        assertTrue(ArchitectureRules.isAllowed(RuntimeBoundary.AI, RuntimeBoundary.TOOL))
    }
}
