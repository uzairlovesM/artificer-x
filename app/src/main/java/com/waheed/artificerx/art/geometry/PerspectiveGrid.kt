package com.waheed.artificerx.art.geometry

data class Point2(val x: Float, val y: Float)
data class PerspectiveGrid(val horizon: Float, val vanishingPoints: List<Point2>, val density: Int = 12)
class PerspectiveGridEngine {
    fun validate(grid: PerspectiveGrid): Boolean = grid.horizon in 0f..1f && grid.vanishingPoints.isNotEmpty() && grid.density in 2..128
}
