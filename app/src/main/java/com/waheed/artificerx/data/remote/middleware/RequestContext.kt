package com.waheed.artificerx.data.remote.middleware

import java.util.UUID

data class RequestContext(val requestId: String = UUID.randomUUID().toString(), val provider: String, val model: String?, val timeoutMs: Long = 60_000L, val tags: Map<String, String> = emptyMap())
