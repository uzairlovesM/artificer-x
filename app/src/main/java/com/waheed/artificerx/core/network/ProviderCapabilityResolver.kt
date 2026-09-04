package com.waheed.artificerx.core.network

import com.waheed.artificerx.domain.model.AiProviderConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderCapabilityResolver @Inject constructor() {
    data class Capabilities(val chat: Boolean, val streaming: Boolean, val tools: Boolean, val vision: Boolean, val embeddings: Boolean, val local: Boolean)
    fun resolve(provider: AiProviderConfig): Capabilities = Capabilities(true, true, true, provider.type.name.contains("LOCAL") || provider.displayName.contains("vision", true), false, provider.type.name.contains("LOCAL"))
}
