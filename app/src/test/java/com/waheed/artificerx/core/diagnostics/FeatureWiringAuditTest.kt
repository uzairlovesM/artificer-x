package com.waheed.artificerx.core.diagnostics

import com.waheed.artificerx.core.agent.ToolRegistry
import com.waheed.artificerx.core.plugin.BuiltinPluginCatalog
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureWiringAuditTest {
    @Test fun registry_has_large_real_capability_surface() {
        assertTrue(ToolRegistry.ALL_TOOLS.size >= 1000)
    }

    @Test fun every_expected_feature_has_plugin_families() {
        val results = FeatureWiringAudit.run()
        assertTrue(results.isNotEmpty())
        assertTrue(results.none { it.missingCapabilities.isNotEmpty() })
        assertTrue(BuiltinPluginCatalog.plugins.size >= 180)
    }
}
