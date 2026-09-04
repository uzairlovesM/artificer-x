package com.waheed.artificerx.ai.drawing

data class DrawingIntent(
    val subject: String,
    val style: String,
    val environment: String,
    val viewpoint: String = "three-quarter",
    val lighting: String = "soft cinematic",
    val requiredObjects: List<String> = emptyList(),
    val exclusions: List<String> = emptyList(),
    val qualityTarget: Float = .92f
)
