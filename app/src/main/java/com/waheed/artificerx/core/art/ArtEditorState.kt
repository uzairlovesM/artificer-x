package com.waheed.artificerx.core.art

import androidx.compose.runtime.Immutable
import com.waheed.artificerx.domain.model.DrawToolType

@Immutable
data class ArtEditorState(
    val selectedBrushId: String = BrushCatalog.presets.first().id,
    val tool: DrawToolType = DrawToolType.BRUSH,
    val zoom: Float = 1f,
    val rotation: Float = 0f,
    val showGrid: Boolean = false,
    val showReference: Boolean = false,
    val symmetry: String = "Off",
    val stabilization: Float = 0.65f,
    val smoothing: Float = 0.4f,
    val fps: Int = 12,
    val onionSkin: Boolean = false,
    val frameIndex: Int = 0,
    val frameCount: Int = 1,
    val screenTone: Boolean = false,
    val clipping: Boolean = false,
    val alphaLock: Boolean = false,
)
