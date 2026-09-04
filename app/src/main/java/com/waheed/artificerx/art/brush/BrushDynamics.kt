package com.waheed.artificerx.art.brush

data class BrushDynamics(val pressureToSize: Float, val pressureToOpacity: Float, val speedToSize: Float, val tiltToAngle: Float, val rotationToPattern: Float, val smoothing: Float)
class BrushDynamicsEngine {
    fun apply(base: Float, pressure: Float, speed: Float, dynamics: BrushDynamics): Float {
        val p = pressure.coerceIn(0f, 1f); val s = (speed/4000f).coerceIn(0f, 1f)
        return (base*(1f+dynamics.pressureToSize*(p-.5f)-dynamics.speedToSize*s*.25f)).coerceAtLeast(.25f)
    }
}
