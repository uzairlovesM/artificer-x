package com.waheed.artificerx.core.util

import kotlin.math.ln
import kotlin.math.pow

object SizeFormatter {
    fun format(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KiB", "MiB", "GiB", "TiB")
        val exponent = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(1, units.lastIndex + 1)
        val value = bytes / 1024.0.pow(exponent.toDouble())
        return "%.2f %s".format(value, units[exponent - 1])
    }
}
