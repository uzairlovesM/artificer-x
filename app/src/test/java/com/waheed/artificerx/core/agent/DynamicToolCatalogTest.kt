package com.waheed.artificerx.core.agent

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DynamicToolCatalogTest {
    @Test fun catalog_exceeds_one_thousand_tools() {
        assertThat(DynamicToolCatalog.tools.size).isAtLeast(1020)
        assertThat(DynamicToolCatalog.tools.map { it.function.name }.distinct().size).isEqualTo(DynamicToolCatalog.tools.size)
    }
}
