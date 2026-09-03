package com.waheed.artificerx.core.art

import kotlinx.serialization.Serializable

@Serializable
data class AnimationFrameMeta(val index: Int, val durationMs: Int = 83, val hold: Boolean = false, val label: String = "Frame")

@Serializable
data class AnimationTimeline(val fps: Int = 12, val loop: Boolean = true, val onionSkin: Boolean = false, val frames: List<AnimationFrameMeta> = listOf(AnimationFrameMeta(0, label = "Frame 1")))
