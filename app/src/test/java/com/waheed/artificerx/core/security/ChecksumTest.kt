package com.waheed.artificerx.core.security
import org.junit.Assert.*
import org.junit.Test

class ChecksumTest {
    @Test fun sha256IsStable() {
        assertEquals(
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
            Checksum.sha256("test")
        )
    }
}
