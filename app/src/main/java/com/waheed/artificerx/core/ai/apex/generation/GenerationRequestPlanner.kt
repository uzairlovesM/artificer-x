package com.waheed.artificerx.core.ai.apex.generation

class GenerationRequestPlanner {
    fun normalize(request: GenerationRequest): GenerationRequest {
        val clean = request.prompt.trim().replace(Regex("\\s+"), " ")
        return request.copy(prompt = clean, parameters = request.parameters.mapKeys { it.key.trim().lowercase() })
    }
    fun requiresSource(kind: GenerationKind): Boolean = kind == GenerationKind.IMAGE_TO_IMAGE || kind == GenerationKind.SKETCH_TO_IMAGE || kind == GenerationKind.VARIATION
}
