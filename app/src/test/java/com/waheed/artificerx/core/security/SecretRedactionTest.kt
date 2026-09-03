package com.waheed.artificerx.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretRedactionTest {
    @Test
    fun redactsCommonCredentialForms() {
        val raw = "Authorization: Bearer sk-TESTONLY-abcdefghijklmnopqrstuvwxyz123456"
        val clean = SecretRedaction.redact(raw)
        assertFalse(clean.contains("sk-TESTONLY-abcdefghijklmnopqrstuvwxyz123456"))
        assertTrue(clean.contains("REDACTED"))
    }
}
