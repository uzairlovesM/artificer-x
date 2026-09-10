package com.waheed.artificerx.core.model

data class ModelRequest(
    val requiredCapabilities: Set<String>,
    val maxSizeBytes: Long,
    val minimumContext: Int
)

class ModelSelector {
    fun select(models: List<ModelDescriptorV2>, request: ModelRequest): ModelDescriptorV2? =
        models.filter {
            it.sizeBytes <= request.maxSizeBytes &&
                it.contextTokens >= request.minimumContext &&
                request.requiredCapabilities.all(it::supports)
        }.maxWithOrNull(compareBy<ModelDescriptorV2> { it.contextTokens }.thenBy { it.sizeBytes })
}
