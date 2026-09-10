package com.waheed.artificerx.core.security

object SecureText {
    fun redact(value: String, visiblePrefix: Int = 3, visibleSuffix: Int = 2): String {
        if (value.isEmpty()) return ""
        if (value.length <= visiblePrefix + visibleSuffix) return "••••"
        val prefix = value.take(visiblePrefix)
        val suffix = value.takeLast(visibleSuffix)
        return "$prefix••••$suffix"
    }

    fun normalizeHeader(value: String): String =
        value.trim().replace(Regex("[\r\n]"), "").take(512)

    fun isProbablySecret(value: String): Boolean {
        val lower = value.lowercase()
        return lower.contains("api_key") || lower.contains("authorization") ||
            lower.startsWith("sk-") || lower.contains("bearer ")
    }
}
