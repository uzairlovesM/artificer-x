package com.waheed.artificerx.core.color

import kotlin.math.max
import kotlin.math.min

object ColorPaletteEngine {
    private data class Hsl(val h: Float, val s: Float, val l: Float)

    fun generate(baseHex: String, harmony: String, count: Int = 5): List<String> {
        val rgb = parseHex(baseHex) ?: return listOf(baseHex.uppercase())
        val base = rgbToHsl(rgb[0], rgb[1], rgb[2])
        val offsets = when (harmony.lowercase()) {
            "analogous" -> listOf(-30f, 0f, 30f, 60f, -60f)
            "triadic" -> listOf(0f, 120f, 240f, 60f, 180f)
            "split_complementary" -> listOf(0f, 150f, 210f, 30f, 330f)
            "tetradic" -> listOf(0f, 90f, 180f, 270f, 45f)
            "monochrome" -> listOf(0f, 0f, 0f, 0f, 0f)
            else -> listOf(0f, 180f, 180f, 0f, 180f)
        }
        return (0 until count.coerceIn(3, 8)).map { i ->
            val lightness = if (harmony.equals("monochrome", true)) (0.20f + i * 0.15f).coerceIn(0.12f, 0.88f) else (base.l + when (i % 3) { 0 -> 0f; 1 -> 0.12f; else -> -0.12f }).coerceIn(0.08f, 0.92f)
            hslToHex(Hsl((base.h + offsets[i % offsets.size] + 360f) % 360f, base.s.coerceIn(0.28f, 1f), lightness))
        }.distinct()
    }

    private fun parseHex(hex: String): IntArray? {
        val c = hex.removePrefix("#")
        if (c.length != 6 || c.any { it.digitToIntOrNull(16) == null }) return null
        return intArrayOf(c.substring(0,2).toInt(16), c.substring(2,4).toInt(16), c.substring(4,6).toInt(16))
    }

    private fun rgbToHsl(r0: Int, g0: Int, b0: Int): Hsl {
        val r=r0/255f; val g=g0/255f; val b=b0/255f; val maxV=max(r,max(g,b)); val minV=min(r,min(g,b)); val d=maxV-minV; val l=(maxV+minV)/2f
        if (d < 1e-5f) return Hsl(0f,0f,l)
        val s=d/(1f-kotlin.math.abs(2f*l-1f))
        val h=when(maxV){r->60f*(((g-b)/d)%6f); g->60f*(((b-r)/d)+2f); else->60f*(((r-g)/d)+4f)}
        return Hsl((h+360f)%360f,s,l)
    }

    private fun hslToHex(hsl:Hsl):String {
        val c=(1f-kotlin.math.abs(2f*hsl.l-1f))*hsl.s; val x=c*(1f-kotlin.math.abs((hsl.h/60f)%2f-1f)); val m=hsl.l-c/2f
        val (r,g,b)=when((hsl.h/60f).toInt()){0->Triple(c,x,0f);1->Triple(x,c,0f);2->Triple(0f,c,x);3->Triple(0f,x,c);4->Triple(x,0f,c);else->Triple(c,0f,x)}
        fun q(v:Float)=((v+m)*255f).toInt().coerceIn(0,255)
        return "#%02X%02X%02X".format(q(r),q(g),q(b))
    }
}
