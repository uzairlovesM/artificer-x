package com.waheed.artificerx.domain.model

/**
 * Identifies which Reasoning Brain provider a ProviderConfig belongs to.
 * CUSTOM covers Section 196's Custom Router flow — any OpenAI-compatible
 * base URL the user points at manually (self-hosted, a provider not yet
 * built in, etc.). LOCAL_GGUF is the fully offline path (Section: Local
 * Model provider) — inference runs in-process against a GGUF file the
 * user imported from their own device storage, with no network call and
 * no AiProviderConfig baseUrl/keyAlias involved at all; it's handled by
 * a dedicated LocalLlamaAdapter branch rather than the shared OpenAI-
 * compatible HTTP path every other provider type uses.
 */
enum class AiProviderType {
    GROQ,
    OPENROUTER,
    CLOUDFLARE_WORKERS_AI,
    CUSTOM,
    LOCAL_GGUF,
}

/**
 * Connection/health state of a configured provider, refreshed by a live
 * ping (Section 195's "Connect" flow: verify the key works before
 * saving it, not after the user's first failed generation).
 */
enum class ProviderConnectionState {
    UNKNOWN,
    TESTING,
    CONNECTED,
    INVALID_KEY,
    RATE_LIMITED,
    UNREACHABLE,
}

/**
 * A single configured Reasoning Brain provider (Section 195/199's
 * ProviderConfig schema). API keys themselves never live in this model
 * as plaintext at rest — the repository layer stores the raw key value
 * in EncryptedSharedPreferences (Section 198) and this model only ever
 * carries a masked preview string for display plus a stable keyAlias
 * used to look the real key up when a call needs it.
 */
data class AiProviderConfig(
    val id: String,
    val type: AiProviderType,
    val displayName: String,
    val baseUrl: String,
    val keyAlias: String,
    val maskedKeyPreview: String,
    val isEnabled: Boolean = true,
    val isPrimary: Boolean = false,
    val supportsVision: Boolean = false,
    val supportsToolCalling: Boolean = true,
    val defaultModelId: String? = null,
    val connectionState: ProviderConnectionState = ProviderConnectionState.UNKNOWN,
    val lastConnectionCheckAtEpochMillis: Long? = null,
    val usageTodayCallCount: Int = 0,
    val lastResetEpochDay: Long = 0L,
    val knownDailyQuota: Int? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
) {
    private val effectiveUsageToday: Int
        get() = if (lastResetEpochDay != System.currentTimeMillis() / 86_400_000L) 0 else usageTodayCallCount

    val isNearQuota: Boolean
        get() = knownDailyQuota != null && effectiveUsageToday >= (knownDailyQuota * 0.85).toInt()

    val isOverQuota: Boolean
        get() = knownDailyQuota != null && effectiveUsageToday >= knownDailyQuota
}

/**
 * Static built-in preset metadata for the provider picker (Section
 * 206's three built-in presets). Not persisted — this describes what
 * the "Connect" card shows before the user has entered anything.
 */
data class AiProviderPreset(
    val type: AiProviderType,
    val displayName: String,
    val description: String,
    val defaultBaseUrl: String,
    val signupUrl: String,
    val supportsVision: Boolean,
    val supportsToolCalling: Boolean,
    val knownDailyQuota: Int?,
    val requiresAccountId: Boolean = false,
)

object AiProviderPresets {
    val GROQ =
        AiProviderPreset(
            type = AiProviderType.GROQ,
            displayName = "Groq",
            description = "Fastest inference (LPU hardware). Free tier, rate-limited per minute/day.",
            defaultBaseUrl = "https://api.groq.com/openai/v1",
            signupUrl = "https://console.groq.com",
            supportsVision = true,
            supportsToolCalling = true,
            knownDailyQuota = null,
        )

    val OPENROUTER =
        AiProviderPreset(
            type = AiProviderType.OPENROUTER,
            displayName = "OpenRouter",
            description = "Aggregates dozens of models. Free-tier list changes weekly — good fallback net.",
            defaultBaseUrl = "https://openrouter.ai/api/v1",
            signupUrl = "https://openrouter.ai/keys",
            supportsVision = true,
            supportsToolCalling = true,
            knownDailyQuota = null,
        )

    val CLOUDFLARE =
        AiProviderPreset(
            type = AiProviderType.CLOUDFLARE_WORKERS_AI,
            displayName = "Cloudflare Workers AI",
            description = "OpenAI-compatible. Free tier ~10,000 neurons/day, no credit card needed.",
            defaultBaseUrl = "https://api.cloudflare.com/client/v4/accounts",
            signupUrl = "https://dash.cloudflare.com/sign-up/workers-ai",
            supportsVision = true,
            supportsToolCalling = true,
            knownDailyQuota = 10000,
            requiresAccountId = true,
        )

    val ALL_PRESETS = listOf(GROQ, OPENROUTER, CLOUDFLARE)

    /** Not a network preset like the others above — this describes the
     *  provider row shown for a successfully imported local model, so
     *  the same "Connect" card / provider-list UI can render it
     *  uniformly. There's no signupUrl or quota; those fields carry
     *  local-appropriate placeholders instead. */
    val LOCAL =
        AiProviderPreset(
            type = AiProviderType.LOCAL_GGUF,
            displayName = "Local Model (On-Device)",
            description = "Your own GGUF model, fully offline. No API key, no network, no quota.",
            defaultBaseUrl = "",
            signupUrl = "",
            supportsVision = true,
            supportsToolCalling = true,
            knownDailyQuota = null,
        )
}
