package com.waheed.artificerx.core.creative

import com.waheed.artificerx.core.art.ColorHarmonyEngine
import com.waheed.artificerx.core.art.HsvColor
import com.waheed.artificerx.core.drawing.BrushDynamics
import com.waheed.artificerx.core.drawing.StrokeGeometry
import com.waheed.artificerx.core.drawing.StrokePoint

data class CreativeAnalysis(
    val strokeCount: Int,
    val totalLength: Float,
    val boundsWidth: Float,
    val boundsHeight: Float,
    val averagePressure: Float,
    val palette: List<HsvColor>
)

class CreativePipeline {
    private val harmony = ColorHarmonyEngine()

    fun analyze(strokes: List<List<StrokePoint>>, seedColor: HsvColor): CreativeAnalysis {
        val all = strokes.flatten()
        val bounds = StrokeGeometry.bounds(all)
        val averagePressure = if (all.isEmpty()) 0f else all.map { it.pressure }.average().toFloat()
        return CreativeAnalysis(
            strokeCount = strokes.size,
            totalLength = strokes.sumOf { StrokeGeometry.length(it).toDouble() }.toFloat(),
            boundsWidth = bounds?.width ?: 0f,
            boundsHeight = bounds?.height ?: 0f,
            averagePressure = averagePressure,
            palette = harmony.analogous(seedColor) + harmony.complementary(seedColor).drop(1)
        )
    }

    fun sampleBrush(size: Float, opacity: Float, pressure: Float): BrushDynamics.Sample =
        BrushDynamics(size, opacity, spacing = .15f).sample(StrokePoint(0f, 0f, pressure))
}
