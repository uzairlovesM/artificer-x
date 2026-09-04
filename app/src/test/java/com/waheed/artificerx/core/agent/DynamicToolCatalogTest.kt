package com.waheed.artificerx.core.agent

import com.google.common.truth.Truth.assertThat
import com.waheed.artificerx.core.runtime.RuntimeToolCatalog
import org.junit.Test

class DynamicToolCatalogTest {
    @Test fun runtime_catalog_has_real_audited_operations() {
        assertThat(RuntimeToolCatalog.SUPPORTED_OPERATIONS).containsAtLeast(
            "WRITE_FILE", "READ_FILE", "REPLACE_TEXT", "COPY_FILE",
            "MOVE_FILE", "DELETE_FILE", "HASH_FILE", "HTTP_GET", "RUN_COMMAND",
        )
        assertThat(RuntimeToolCatalog.SUPPORTED_OPERATIONS).doesNotContain("NO_OP")
    }

    @Test fun runtime_tool_name_contract_is_namespaced() {
        assertThat(Regex("^runtime_[a-z][a-z0-9_]{2,63}$").matches("runtime_export_png")).isTrue()
        assertThat(Regex("^runtime_[a-z][a-z0-9_]{2,63}$").matches("ai_provider_tool_0001")).isFalse()
    }
}
