package com.waheed.artificerx.core.art
import kotlin.math.abs
data class HsvColor(val h: Float, val s: Float, val v: Float)
class ColorHarmonyEngine {
    fun complementary(c:HsvColor)=listOf(c,HsvColor((c.h+180f)%360f,c.s,c.v))
    fun analogous(c:HsvColor, spread:Float=30f)=listOf(
        HsvColor((c.h-spread+360f)%360f,c.s,c.v), c, HsvColor((c.h+spread)%360f,c.s,c.v)
    )
    fun triadic(c:HsvColor)=listOf(c,HsvColor((c.h+120f)%360f,c.s,c.v),HsvColor((c.h+240f)%360f,c.s,c.v))
    fun contrast(a:HsvColor,b:HsvColor):Float {
        val hue=minOf(abs(a.h-b.h),360f-abs(a.h-b.h))/180f
        return hue*.6f+abs(a.s-b.s)*.2f+abs(a.v-b.v)*.2f
    }
}
