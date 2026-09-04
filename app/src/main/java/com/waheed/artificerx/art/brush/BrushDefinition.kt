package com.waheed.artificerx.art.brush

data class BrushDefinition(
    val id: String,
    val name: String,
    val size: Float,
    val opacity: Float,
    val spacing: Float,
    val hardness: Float,
    val flow: Float,
    val angleFollow: Float,
    val pressureSize: Float,
    val pressureOpacity: Float,
    val jitter: Float,
    val textureId: String? = null,
    val stabilizer: Float = 0f
)
