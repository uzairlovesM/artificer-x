package com.waheed.artificerx.core.diagnostics

import java.io.File
import java.security.MessageDigest

/** Runtime-safe integrity checks for imported project bundles and local caches. */
class BuildIntegrityGate {
    data class Verification(
        val readable: Boolean,
        val regularFile: Boolean,
        val byteSize: Long,
        val sha256: String?,
    )

    fun verify(file: File): Verification {
        if (!file.exists()) return Verification(false, false, 0L, null)
        if (!file.isFile) return Verification(false, false, file.length(), null)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            var read: Int
            while (input.read(buffer).also { read = it } >= 0) {
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return Verification(file.canRead(), true, file.length(), digest.digest().joinToString("") { "%02x".format(it) })
    }

    companion object { private const val DEFAULT_BUFFER = 32 * 1024 }
}
