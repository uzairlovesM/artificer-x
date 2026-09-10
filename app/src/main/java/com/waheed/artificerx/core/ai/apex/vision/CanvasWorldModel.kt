package com.waheed.artificerx.core.ai.apex.vision

data class SceneRegion(val id: String, val left: Float, val top: Float, val right: Float, val bottom: Float, val semantic: String, val confidence: Float) {
    init { require(right >= left && bottom >= top); require(confidence in 0f..1f) }
}

data class CanvasWorldSnapshot(val revision: Long, val width: Int, val height: Int, val regions: List<SceneRegion>, val paletteArgb: List<Int>, val metadata: Map<String, String>)

class CanvasWorldModel {
    private var value = CanvasWorldSnapshot(0L, 1, 1, emptyList(), emptyList(), emptyMap())
    @Synchronized fun update(snapshot: CanvasWorldSnapshot) { require(snapshot.revision >= value.revision); value = snapshot }
    @Synchronized fun snapshot(): CanvasWorldSnapshot = value
    @Synchronized fun revision(): Long = value.revision
    fun findRegion(label: String): SceneRegion? = snapshot().regions.filter { it.semantic.contains(label, true) }.maxByOrNull { it.confidence }
}
