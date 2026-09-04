package com.waheed.artificerx.core.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatProfile(
    val id: String,
    val name: String,
    val providerId: String? = null,
    val modelId: String? = null,
    val temperature: Double = 0.35,
    val reasoningEnabled: Boolean = true,
    val webResearch: Boolean = true,
    val creativeAutonomy: Boolean = true,
    val contextMode: ContextMode = ContextMode.DEEP,
)

@Serializable
enum class ContextMode { COMPACT, LARGE, DEEP }
