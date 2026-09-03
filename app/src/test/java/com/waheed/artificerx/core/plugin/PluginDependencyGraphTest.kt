package com.waheed.artificerx.core.plugin

import org.junit.Assert.assertTrue
import org.junit.Test

class PluginDependencyGraphTest {
    @Test
    fun builtInDependenciesHaveNoCyclesAndResolve() {
        assertTrue(PluginDependencyGraph.cycles().isEmpty())
        assertTrue(PluginDependencyGraph.missingFromCatalog(BuiltinPluginCatalog.plugins.map { it.id }.toSet()).isEmpty())
    }
}
