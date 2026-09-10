package com.waheed.artificerx.core

import com.waheed.artificerx.ai.AiPipelineRuntime
import com.waheed.artificerx.art.ArtRuntime
import com.waheed.artificerx.data.DataIntegrityRuntime
import com.waheed.artificerx.domain.DomainRuntime
import com.waheed.artificerx.util.UtilityRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeepRuntimeSmokeTest {
    @Test
    fun aiRoutingPrefersHigherWeightedSignal() {
        val runtime = AiPipelineRuntime()
        runtime.transition(AiPipelineRuntime.Stage.CONTEXT)
        runtime.transition(AiPipelineRuntime.Stage.ROUTING)
        val result = runtime.route(
            mapOf(
                "local" to listOf(AiPipelineRuntime.Signal("quality", 2.0, 0.95)),
                "remote" to listOf(AiPipelineRuntime.Signal("quality", 1.0, 0.99)),
            ),
        )
        assertEquals("local", result.route)
    }

    @Test
    fun artRuntimePreservesStrokeGeometry() {
        val runtime = ArtRuntime()
        runtime.begin(ArtRuntime.StrokeSample(0f, 0f, 0.5f, 10))
        runtime.add(ArtRuntime.StrokeSample(3f, 4f, 1f, 20))
        assertEquals(5f, runtime.end()!!.length, 0.0001f)
    }

    @Test
    fun dataIntegrityValidatesManifestChecksum() {
        val runtime = DataIntegrityRuntime()
        val checksum = runtime.checksum("artificer".toByteArray())
        assertTrue(runtime.validate(listOf(DataIntegrityRuntime.ManifestEntry("asset.bin", 9, checksum))).valid)
    }

    @Test
    fun domainRejectsOversizedProjects() {
        val runtime = DomainRuntime()
        val errors = runtime.validate(
            DomainRuntime.ProjectDimensions(10_000, 10_000),
            DomainRuntime.ProjectPolicy(20_000_000, true, true),
        )
        assertTrue(errors.contains("pixel-budget-exceeded"))
    }

    @Test
    fun utilityCrc32MatchesKnownVector() {
        assertEquals(0x352441C2L, UtilityRuntime().crc32("abc".toByteArray()))
    }
}
