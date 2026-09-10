package com.waheed.artificerx.core.canvas

import kotlin.math.ceil
import kotlin.math.floor

data class Tile(val x: Int, val y: Int, val size: Int)

class TilePlanner(private val tileSize: Int = 256) {
    init { require(tileSize > 0) }

    fun visibleTiles(left: Float, top: Float, right: Float, bottom: Float): List<Tile> {
        val minX = floor(left / tileSize).toInt()
        val minY = floor(top / tileSize).toInt()
        val maxX = ceil(right / tileSize).toInt()
        val maxY = ceil(bottom / tileSize).toInt()
        val result = ArrayList<Tile>()
        for (y in minY..maxY) for (x in minX..maxX) result += Tile(x, y, tileSize)
        return result
    }
}
