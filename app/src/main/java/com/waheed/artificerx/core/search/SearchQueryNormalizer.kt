package com.waheed.artificerx.core.search

class SearchQueryNormalizer {
    fun normalize(input: String): String =
        input.trim()
            .replace(Regex("\s+"), " ")
            .replace(Regex("[\u0000-\u001F]"), "")
            .take(4_000)

    fun terms(input: String): List<String> =
        normalize(input).lowercase().split(' ').filter { it.length >= 2 }.distinct()
}
