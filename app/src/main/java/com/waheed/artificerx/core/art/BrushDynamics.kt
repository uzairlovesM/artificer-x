package com.waheed.artificerx.core.art

import com.waheed.artificerx.domain.model.BrushType
import kotlin.math.max
import kotlin.math.min

data class BrushDynamics(val size: Float, val opacity: Float, val spacing: Float, val angle: Float, val scatter: Float)
object BrushDynamicsEngine {
    fun evaluate(type: BrushType, speed: Float, pressure: Float): BrushDynamics {
        val slow = (1f-speed.coerceIn(0f, 1f)); val p = pressure.coerceIn(0f, 1f)
        return when(type){
            BrushType.PENCIL -> BrushDynamics(0.75f+1.1f*p, 0.45f+0.45f*p, 0.12f, 0f, 0.02f)
            BrushType.INK_PEN -> BrushDynamics(0.70f+1.5f*p, 0.65f+0.35f*p, 0.04f, 0f, 0f)
            BrushType.MARKER -> BrushDynamics(0.9f+0.8f*slow, 0.35f+0.4f*p, 0.08f, 0f, 0.01f)
            BrushType.CALLIGRAPHY -> BrushDynamics(0.8f+1.4f*p, 0.55f+0.4f*p, 0.05f, 20f, 0.0f)
            BrushType.AIRBRUSH -> BrushDynamics(1.4f+2f*p, 0.12f+0.30f*p, 0.02f, 0f, 0.12f)
            BrushType.CHARCOAL -> BrushDynamics(1.8f+1.2f*p, 0.20f+0.4f*p, 0.17f, 0f, 0.18f)
            BrushType.WATERCOLOR -> BrushDynamics(1.2f+2.2f*p, 0.15f+0.5f*p, 0.20f, 0f, 0.05f)
            BrushType.ERASER_SOFT -> BrushDynamics(1.4f+2f*p, 1f, 0.04f, 0f, 0.0f)
        }.let{it.copy(size = min(5f, max(0.1f, it.size)))}
    }
}
