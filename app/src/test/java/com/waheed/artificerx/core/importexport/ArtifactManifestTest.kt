package com.waheed.artificerx.core.importexport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ArtifactManifestTest {
    @Test
    fun codecRoundTripsManifest() {
        val source = ArtifactManifest(entries = listOf(ArtifactManifestEntry("a.txt", "text/plain", 3, "abc")))
        val decoded = ArtifactManifestCodec.decode(ArtifactManifestCodec.encode(source))
        assertNotNull(decoded)
        assertEquals(source.entries, decoded!!.entries)
    }
}
