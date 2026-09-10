package com.waheed.artificerx.core.drawing.engine

data class StrokeSample(
    val x: Float,
    val y: Float,
    val pressure: Float = 1f,
    val timestampNanos: Long = 0L,
    val tilt: Float = 0f,
    val azimuth: Float = 0f,
)

fun List<Float>.toStrokeSamples(pressures: List<Float>? = null): List<StrokeSample> {
    if (size < 2 || size % 2 != 0) return emptyList()
    val pointCount = size / 2
    return List(pointCount) { index ->
        StrokeSample(
            x = this[index * 2],
            y = this[index * 2 + 1],
            pressure = pressures?.getOrNull(index)?.coerceIn(0f, 1f) ?: 1f,
        )
    }
}

fun List<StrokeSample>.toFloatPoints(): List<Float> = flatMap { listOf(it.x, it.y) }
