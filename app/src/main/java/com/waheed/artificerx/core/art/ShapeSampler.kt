package com.waheed.artificerx.core.art
import kotlin.math.cos
import kotlin.math.sin
data class SampledShape(val points:List<Point2>)
class ShapeSampler {
    fun circle(cx:Float,cy:Float,radius:Float,segments:Int=64):SampledShape {
        require(radius>=0f); require(segments>=3)
        return SampledShape((0 until segments).map{ i ->
            val a=2.0*Math.PI*i/segments
            Point2(cx+cos(a).toFloat()*radius,cy+sin(a).toFloat()*radius)
        })
    }
    fun rectangle(left:Float,top:Float,right:Float,bottom:Float):SampledShape =
        SampledShape(listOf(Point2(left,top),Point2(right,top),Point2(right,bottom),Point2(left,bottom),Point2(left,top)))
}
