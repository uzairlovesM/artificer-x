package com.waheed.artificerx.core.routing

import com.waheed.artificerx.domain.model.AiProviderConfig
import com.waheed.artificerx.domain.model.AiProviderType

/** Deterministic provider routing. It prefers usable capability before primary status,
 * so a primary provider that cannot satisfy a vision/tool request does not block a better fit. */
object ModelRoutingPolicy {
    data class RequestNeeds(
        val vision: Boolean,
        val toolCalling: Boolean = true,
        val offlineOnly: Boolean = false,
    )

    fun rank(providers: List<AiProviderConfig>, needs: RequestNeeds): List<AiProviderConfig> =
        providers.sortedWith(
            compareByDescending<AiProviderConfig> { compatibilityScore(it, needs) }
                .thenByDescending { it.isPrimary }
                .thenBy { it.isOverQuota }
                .thenBy { it.isNearQuota },
        )

    private fun compatibilityScore(provider: AiProviderConfig, needs: RequestNeeds): Int {
        if (needs.offlineOnly) return if (provider.type == AiProviderType.LOCAL_GGUF) 1_000 else -1_000
        var score = 0
        if (needs.vision) score += if (provider.supportsVision) 120 else -300
        if (needs.toolCalling) score += if (provider.supportsToolCalling) 100 else -250
        if (provider.type == AiProviderType.LOCAL_GGUF) score += 15
        if (provider.isOverQuota) score -= 80
        else if (provider.isNearQuota) score -= 20
        return score
    }
}
