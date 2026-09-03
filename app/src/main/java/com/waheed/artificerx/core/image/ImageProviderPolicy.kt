package com.waheed.artificerx.core.image

import com.waheed.artificerx.domain.model.AiProviderConfig
import com.waheed.artificerx.domain.model.AiProviderType

/** Orders configured providers for image generation without adding another secrets store. */
object ImageProviderPolicy {
    fun rank(providers: List<AiProviderConfig>): List<AiProviderConfig> = providers
        .filter { it.isEnabled && it.type != AiProviderType.LOCAL_GGUF }
        .sortedWith(
            compareByDescending<AiProviderConfig> { it.isPrimary }
                .thenBy { it.isOverQuota }
                .thenBy { it.isNearQuota }
                .thenByDescending { it.supportsVision },
        )
}
