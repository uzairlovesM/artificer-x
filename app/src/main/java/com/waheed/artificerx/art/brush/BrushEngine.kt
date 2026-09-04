package com.waheed.artificerx.art.brush

import kotlin.math.max
import kotlin.math.min

class BrushEngine {
    fun normalize(input: BrushDefinition): BrushDefinition = input.copy(
        size = input.size.coerceIn(.25f, 4096f),
        opacity = input.opacity.coerceIn(0f, 1f),
        spacing = input.spacing.coerceIn(.01f, 4f),
        hardness = input.hardness.coerceIn(0f, 1f),
        flow = input.flow.coerceIn(0f, 1f),
        angleFollow = input.angleFollow.coerceIn(0f, 1f),
        pressureSize = input.pressureSize.coerceIn(0f, 1f),
        pressureOpacity = input.pressureOpacity.coerceIn(0f, 1f),
        jitter = input.jitter.coerceIn(0f, 1f),
        stabilizer = input.stabilizer.coerceIn(0f, 1f)
    )

    fun pressureAdjustedSize(base: Float, pressure: Float, sensitivity: Float): Float {
        val p = min(1f, max(0f, pressure))
        return base * (1f - sensitivity * .85f + sensitivity * .85f * p)
    }
}
