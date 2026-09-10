package com.waheed.artificerx.core.image

object PixelMath {
    fun clamp8(value: Int): Int = value.coerceIn(0, 255)
    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (clamp8(a) shl 24) or (clamp8(r) shl 16) or (clamp8(g) shl 8) or clamp8(b)

    fun blendOver(dst: Int, src: Int): Int {
        val sa = (src ushr 24) and 255
        if (sa == 255) return src
        if (sa == 0) return dst
        val da = (dst ushr 24) and 255
        val sr = (src ushr 16) and 255; val sg = (src ushr 8) and 255; val sb = src and 255
        val dr = (dst ushr 16) and 255; val dg = (dst ushr 8) and 255; val db = dst and 255
        val outA = sa + (da * (255 - sa) + 127) / 255
        fun channel(s: Int, d: Int): Int = (s * sa + d * da * (255 - sa) / 255 + outA / 2) / outA.coerceAtLeast(1)
        return argb(outA, channel(sr, dr), channel(sg, dg), channel(sb, db))
    }
}
