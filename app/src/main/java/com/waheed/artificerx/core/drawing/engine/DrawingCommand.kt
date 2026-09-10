package com.waheed.artificerx.core.drawing.engine

import java.util.UUID

data class DrawingCommand(
    val id: String = UUID.randomUUID().toString(),
    val layerId: String,
    val points: List<Float>,
    val colorHex: String,
    val width: Float,
    val opacity: Float,
    val brushId: String,
    val weights: List<Float> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
)
