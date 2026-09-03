package com.waheed.artificerx.core.security

/** Removes common API-key/token shapes before sensitive text is persisted into logs, previews, or exports. */
object SecretRedaction {
    private val patterns = listOf(
        Regex("(?i)\\bsk-[A-Za-z0-9_-]{12,}\\b") to "[REDACTED_API_KEY]",
        Regex("(?i)\\bghp_[A-Za-z0-9]{20,}\\b") to "[REDACTED_GITHUB_TOKEN]",
        Regex("(?i)\\bAIza[0-9A-Za-z_-]{20,}\\b") to "[REDACTED_GOOGLE_KEY]",
        Regex("(?i)\\bBearer\\s+[A-Za-z0-9._-]{16,}") to "Bearer [REDACTED_TOKEN]",
        Regex("(?i)(api[_-]?key\\s*[=:]\\s*)[^\\s,;]+") to "$1[REDACTED]",
    )

    fun redact(text: String, maxLength: Int = 50_000): String = patterns.fold(text.take(maxLength)) { acc, (regex, replacement) -> regex.replace(acc, replacement) }
}
