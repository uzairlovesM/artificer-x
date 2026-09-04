package com.waheed.artificerx.domain.usecase

import com.waheed.artificerx.core.chat.ChatProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildAdaptiveAiContextUseCase @Inject constructor() {
    fun budget(profile: ChatProfile, conversationChars: Int, artifactChars: Int, toolCount: Int): Int {
        val profileBase = when (profile.contextMode.name) { "COMPACT" -> 48000; "LARGE" -> 140000; else -> 260000 }
        return (profileBase + artifactChars / 2 + toolCount * 900 - conversationChars / 4).coerceAtLeast(32000)
    }
}
