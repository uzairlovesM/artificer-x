package com.waheed.artificerx.core.security

import java.security.MessageDigest

object Checksum {
    fun sha256(bytes: ByteArray): String = digest("SHA-256", bytes)
    fun sha512(bytes: ByteArray): String = digest("SHA-512", bytes)

    fun sha256(text: String): String = sha256(text.toByteArray(Charsets.UTF_8))

    private fun digest(algorithm: String, bytes: ByteArray): String =
        MessageDigest.getInstance(algorithm).digest(bytes).joinToString("") { "%02x".format(it) }

    fun constantTimeEquals(a: String, b: String): Boolean {
        val aa = a.toByteArray(Charsets.UTF_8)
        val bb = b.toByteArray(Charsets.UTF_8)
        var result = aa.size xor bb.size
        val size = minOf(aa.size, bb.size)
        for (i in 0 until size) result = result or (aa[i].toInt() xor bb[i].toInt())
        return result == 0
    }
}
