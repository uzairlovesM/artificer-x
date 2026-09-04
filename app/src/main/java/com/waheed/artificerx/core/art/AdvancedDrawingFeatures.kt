package com.waheed.artificerx.core.art

import kotlinx.serialization.Serializable

@Serializable
data class AdvancedDrawingFeatures(
    val stabilizerMode: StabilizerMode = StabilizerMode.REAL_TIME,
    val stabilization: Float = 0.65f,
    val symmetrySlices: Int = 1,
    val perspectiveGuides: Int = 1,
    val mirrorMode: MirrorMode = MirrorMode.NONE,
    val screenToneEnabled: Boolean = false,
    val clippingEnabled: Boolean = false,
    val alphaLockEnabled: Boolean = false,
    val vectorMode: Boolean = false,
    val selectionMode: SelectionMode = SelectionMode.RECTANGLE,
    val gridEnabled: Boolean = false,
    val onionSkinEnabled: Boolean = false,
)
enum class StabilizerMode { OFF, REAL_TIME, AFTER }
enum class MirrorMode { NONE, HORIZONTAL, VERTICAL, BOTH }
enum class SelectionMode { RECTANGLE, ELLIPSE, LASSO, COLOR_RANGE }
