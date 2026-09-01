package com.waheed.artificerx.core.agent

sealed class AgentEvent {
    data class ThinkingStarted(
        val providerName: String,
    ) : AgentEvent()

    data class ToolCallStarted(
        val callId: String,
        val toolName: String,
        val argsPreview: String,
    ) : AgentEvent()

    data class ToolCallSucceeded(
        val callId: String,
        val resultSummary: String,
    ) : AgentEvent()

    data class ToolCallFailed(
        val callId: String,
        val errorMessage: String,
    ) : AgentEvent()

    data class AgentTextChunk(
        val text: String,
    ) : AgentEvent()

    data class TurnCompleted(
        val summary: String,
    ) : AgentEvent()

    data class ProviderFallback(
        val fromProvider: String,
        val toProvider: String,
        val reason: String,
    ) : AgentEvent()

    data class Error(
        val message: String,
        val isFatal: Boolean,
    ) : AgentEvent()

    object MaxIterationsReached : AgentEvent()

    /** Section "Critic" role's verdict on completed work — approved()
     *  is true when the Critic's finish_turn response started with
     *  "APPROVED:", false for "NEEDS_REPAIR:" (see AgentOrchestrator.
     *  runCriticPass). [detail] is the Critic's reasoning either way. */
    data class CriticReview(
        val approved: Boolean,
        val detail: String,
    ) : AgentEvent()

    /** Emitted when a failed Critic review triggers an automatic Repair
     *  pass, so the UI can show the user why another round of tool
     *  calls is happening after the turn already looked finished. */
    data class RepairStarted(
        val issue: String,
    ) : AgentEvent()

    /** Section: Local Model provider — the on-device engine reports
     *  load progress before the first token can be generated (reading
     *  a multi-GB GGUF file off flash + building the KV cache takes
     *  real, user-visible time, unlike a remote provider's near-
     *  instant HTTP round trip). [phase] is a short human label
     *  ("Reading model file", "Loading vision projector",
     *  "Warming up context"); [progressFraction] is 0f..1f when known,
     *  null when the underlying stage doesn't report granular progress. */
    data class LocalModelLoading(
        val phase: String,
        val progressFraction: Float?,
    ) : AgentEvent()

    /** Local inference throughput, sampled periodically during
     *  generation — shown as a small "N tok/s" indicator so the user
     *  has a concrete signal of whether their chosen quantization/
     *  context/thread settings are actually usable on their hardware,
     *  something a remote provider gives no equivalent visibility into. */
    data class LocalModelSpeed(
        val tokensPerSecond: Double,
    ) : AgentEvent()
}
