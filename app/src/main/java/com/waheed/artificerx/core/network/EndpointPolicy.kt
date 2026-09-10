package com.waheed.artificerx.core.network

import java.net.URI

class EndpointPolicy(
    private val allowedSchemes: Set<String> = setOf("https"),
    private val blockedHosts: Set<String> = emptySet()
) {
    fun validate(url: String): Result<URI> = runCatching {
        val uri = URI(url.trim())
        require(uri.scheme?.lowercase() in allowedSchemes) { "Unsupported URL scheme" }
        val host = uri.host?.lowercase() ?: error("URL host is missing")
        require(host !in blockedHosts) { "Host is blocked by policy" }
        require(uri.userInfo == null) { "Userinfo in URL is not allowed" }
        uri
    }
}
