package com.waheed.artificerx.core.ai.apex.generation

enum class GenerationKind { IMAGE, IMAGE_TO_IMAGE, SKETCH_TO_IMAGE, VARIATION }
data class GenerationRequest(val kind: GenerationKind, val prompt: String, val sourceImage: IntArray? = null, val seed: Long? = null, val parameters: Map<String, String> = emptyMap())
data class GeneratedArtifact(val mime: String, val width: Int, val height: Int, val pixels: IntArray?, val providerId: String, val metadata: Map<String, String> = emptyMap())
interface GenerationAdapter { val id: String; fun supports(kind: GenerationKind): Boolean; fun generate(request: GenerationRequest): Result<GeneratedArtifact> }
class GenerationGateway(private val adapters: MutableList<GenerationAdapter> = mutableListOf()) {
    fun register(adapter: GenerationAdapter) { adapters.removeAll { it.id == adapter.id }; adapters += adapter }
    fun generate(request: GenerationRequest): Result<GeneratedArtifact> {
        require(request.prompt.isNotBlank())
        val capable = adapters.filter { it.supports(request.kind) }
        if (capable.isEmpty()) return Result.failure(IllegalStateException("No generation adapter registered for ${request.kind}"))
        var last: Result<GeneratedArtifact> = Result.failure(IllegalStateException("no_attempt"))
        for (adapter in capable) { last = adapter.generate(request); if (last.isSuccess) return last }
        return last
    }
}
