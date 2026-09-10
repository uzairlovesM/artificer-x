package com.waheed.artificerx.core.art
data class SelectionRegion(val x: Int, val y: Int, val width: Int, val height: Int) {
    init { require(width >= 0 && height >= 0) }
    fun contains(px: Int, py: Int) = px >= x && py >= y && px < x + width && py < y + height
    fun intersect(other: SelectionRegion): SelectionRegion? {
        val l=maxOf(x,other.x); val t=maxOf(y,other.y); val r=minOf(x+width,other.x+other.width); val b=minOf(y+height,other.y+other.height)
        return if (r>l && b>t) SelectionRegion(l,t,r-l,b-t) else null
    }
}
class SelectionEngineV2 {
    fun union(regions: List<SelectionRegion>): SelectionRegion? {
        if (regions.isEmpty()) return null
        val l=regions.minOf{it.x}; val t=regions.minOf{it.y}; val r=regions.maxOf{it.x+it.width}; val b=regions.maxOf{it.y+it.height}
        return SelectionRegion(l,t,r-l,b-t)
    }
}
