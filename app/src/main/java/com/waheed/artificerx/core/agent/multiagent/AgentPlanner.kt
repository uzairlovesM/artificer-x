package com.waheed.artificerx.core.agent.multiagent

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chooses which [AgentRole] focus applies to a user turn.
 *
 * Deliberately a keyword/heuristic classifier rather than a second LLM
 * call: an extra classification round-trip before every single turn
 * would double API usage against the same free-tier daily quotas
 * ([com.waheed.artificerx.domain.model.AiProviderConfig.isOverQuota]
 * exists precisely because those quotas are tight), for a benefit
 * (picking the "best" specialist framing) that's marginal compared to
 * [AgentRole.MASTER] handling most turns directly. Explicit signals
 * ("light the scene better", "keep the character consistent") are
 * strong and cheap to detect this way; anything ambiguous falls back
 * to MASTER, which is a safe, fully-capable default.
 */
@Singleton
class AgentPlanner
    @Inject
    constructor() {
        fun selectRole(
            userText: String,
            is3DMode: Boolean,
        ): AgentRole {
            val lower = userText.lowercase()

            return when {
                CHARACTER_SIGNALS.any { it in lower } -> AgentRole.CHARACTER
                LIGHTING_SIGNALS.any { it in lower } -> AgentRole.LIGHTING
                COMPOSITION_SIGNALS.any { it in lower } -> AgentRole.COMPOSITION
                ENVIRONMENT_SIGNALS.any { it in lower } -> AgentRole.ENVIRONMENT
                STYLE_SIGNALS.any { it in lower } -> AgentRole.ART_DIRECTOR
                is3DMode -> AgentRole.RENDERING
                else -> AgentRole.MASTER
            }
        }

        private companion object {
            val CHARACTER_SIGNALS =
                listOf(
                    "character",
                    "same face",
                    "consistent look",
                    "recurring",
                    "the same person",
                    "the same cat",
                    "the same dog",
                    "protagonist",
                    "mascot",
                )
            val LIGHTING_SIGNALS =
                listOf(
                    "light",
                    "lighting",
                    "shadow",
                    "shading",
                    "glow",
                    "backlit",
                    "silhouette",
                    "highlight",
                )
            val COMPOSITION_SIGNALS =
                listOf(
                    "compose",
                    "composition",
                    "layout",
                    "framing",
                    "balance",
                    "rule of thirds",
                    "focal point",
                )
            val ENVIRONMENT_SIGNALS =
                listOf(
                    "background",
                    "setting",
                    "environment",
                    "scenery",
                    "landscape",
                    "sky",
                    "location",
                )
            val STYLE_SIGNALS =
                listOf(
                    "style",
                    "mood",
                    "palette",
                    "color scheme",
                    "art direction",
                    "aesthetic",
                    "vibe",
                )
        }
    }
