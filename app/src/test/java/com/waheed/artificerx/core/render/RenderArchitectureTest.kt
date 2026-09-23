package com.waheed.artificerx.core.render

import org.junit.Assert.assertTrue
import org.junit.Test

class RenderArchitectureTest {
    @Test
    fun allAgslEffectsDeclareInputShader() {
        RuntimeShaderEffects.ShaderEffect.values().forEach { effect ->
            assertTrue("${effect.name} must sample inputImage", effect.source.contains("inputImage"))
        }
    }

    @Test
    fun allAgslEffectsDeclareResolutionAndIntensity() {
        RuntimeShaderEffects.ShaderEffect.values().forEach { effect ->
            assertTrue("${effect.name} missing intensity", effect.source.contains("uniform float intensity"))
            assertTrue("${effect.name} missing resolution", effect.source.contains("uniform float2 resolution"))
        }
    }
}
