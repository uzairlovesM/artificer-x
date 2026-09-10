package com.waheed.artificerx.core.art
import kotlin.math.cos
import kotlin.math.sin
data class Point2(val x:Float,val y:Float)
data class Transform2D(val tx:Float=0f,val ty:Float=0f,val scaleX:Float=1f,val scaleY:Float=1f,val rotationDegrees:Float=0f) {
    fun apply(p:Point2):Point2 {
        val r=Math.toRadians(rotationDegrees.toDouble()); val c=cos(r).toFloat(); val s=sin(r).toFloat()
        val x=p.x*scaleX; val y=p.y*scaleY
        return Point2(x*c-y*s+tx,x*s+y*c+ty)
    }
    fun combine(other:Transform2D):Transform2D = Transform2D(
        tx+other.tx,ty+other.ty,scaleX*other.scaleX,scaleY*other.scaleY,rotationDegrees+other.rotationDegrees
    )
}
