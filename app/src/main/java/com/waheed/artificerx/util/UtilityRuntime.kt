package com.waheed.artificerx.util

import com.waheed.artificerx.core.foundation.SubsystemHealth
import java.nio.ByteBuffer
import java.nio.ByteOrder

class UtilityRuntime {
    fun crc32(bytes: ByteArray): Long {
        var crc = 0xFFFFFFFFL
        bytes.forEach { value ->
            crc = crc xor (value.toInt() and 0xFF).toLong()
            repeat(8) { _ -> crc = if ((crc and 1L) != 0L) (crc ushr 1) xor 0xEDB88320L else crc ushr 1 }
        }
        return crc xor 0xFFFFFFFFL
    }

    fun stableFloatBytes(values: List<Float>): ByteArray = ByteBuffer.allocate(values.size * 4).order(ByteOrder.LITTLE_ENDIAN).apply { values.forEach { putFloat(it) } }.array()

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "util",
        readiness = 1.0,
        capabilities = setOf("crc32", "stable-binary-encoding"),
        invariants = listOf("little-endian-float-format", "deterministic-checksum"),
        counters = emptyMap(),
    )
}
