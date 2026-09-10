package com.waheed.artificerx.core.model

data class ModelDescriptorV2(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val contextTokens: Int,
    val capabilities: Set<String>,
    val quantization: String? = null
) {
    init {
        require(id.isNotBlank() && displayName.isNotBlank())
        require(sizeBytes > 0)
        require(contextTokens > 0)
    }

    fun supports(capability: String): Boolean = capability.lowercase() in capabilities.map(String::lowercase)
}
