package com.waheed.artificerx.util.serialization

import java.security.MessageDigest

object StableId {
    fun of(vararg parts: String): String {
        val input = parts.joinToString("\u001f")
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
