package com.waheed.artificerx.core.ai.apex.generation

/** Deterministic fallback adapter: creates a simple raster artifact so workflows can remain testable offline. */
class DeterministicGenerationAdapter : GenerationAdapter {
    override val id: String = "deterministic-offline"
    override fun supports(kind: GenerationKind): Boolean = kind in setOf(GenerationKind.IMAGE, GenerationKind.VARIATION)
    override fun generate(request: GenerationRequest): Result<GeneratedArtifact> = runCatching {
        val width = 256; val height = 256
        val pixels = IntArray(width * height)
        val seed = request.seed ?: request.prompt.hashCode().toLong()
        var x = seed xor -7046029254386353131L
        for (i in pixels.indices) { x = x * -7046029254386353131L + 3037000493L; val v = (x ushr 32).toInt() and 0xff; pixels[i] = 0xff000000.toInt() or (v shl 16) or ((255 - v) shl 8) or v }
        GeneratedArtifact("image/raw-argb", width, height, pixels, id, mapOf("deterministic" to "true", "promptHash" to request.prompt.hashCode().toString()))
    }
}
