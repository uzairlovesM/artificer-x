package com.waheed.artificerx.data

import com.waheed.artificerx.core.foundation.SubsystemHealth
import java.security.MessageDigest

class DataIntegrityRuntime {
    data class ManifestEntry(val path: String, val size: Long, val checksum: String)
    data class Validation(val valid: Boolean, val errors: List<String>)

    fun checksum(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun validate(entries: List<ManifestEntry>): Validation {
        val errors = mutableListOf<String>()
        val duplicate = entries.groupingBy { it.path }.eachCount().filterValues { it > 1 }.keys
        duplicate.forEach { errors += "duplicate:$it" }
        entries.forEach {
            if (it.path.isBlank()) errors += "blank-path"
            if (it.size < 0) errors += "negative-size:${it.path}"
            if (!it.checksum.matches(Regex("[0-9a-fA-F]{64}"))) errors += "invalid-checksum:${it.path}"
        }
        return Validation(errors.isEmpty(), errors.distinct())
    }

    fun inspect(): SubsystemHealth = SubsystemHealth(
        id = "data",
        readiness = 1.0,
        capabilities = setOf("sha256", "manifest-validation", "duplicate-detection"),
        invariants = listOf("stable-checksum-format", "unique-manifest-paths", "non-negative-size"),
        counters = emptyMap(),
    )
}
