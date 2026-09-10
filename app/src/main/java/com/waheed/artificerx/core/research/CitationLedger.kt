package com.waheed.artificerx.core.research

data class Citation(val id: String, val url: String, val claim: String, val sourceTitle: String)

class CitationLedger {
    private val entries = LinkedHashMap<String, Citation>()

    fun add(citation: Citation) {
        require(citation.id.isNotBlank())
        require(citation.url.isNotBlank())
        entries[citation.id] = citation
    }

    fun get(id: String): Citation? = entries[id]
    fun all(): List<Citation> = entries.values.toList()
    fun unresolvedClaims(claims: List<String>): List<String> =
        claims.filter { claim -> entries.values.none { it.claim == claim } }
}
