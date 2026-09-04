package com.waheed.artificerx.data.remote.client

import com.waheed.artificerx.data.remote.middleware.RequestContext

data class RemoteResponse(val status: Int, val headers: Map<String, String>, val body: String)
interface RemoteClient { suspend fun execute(context: RequestContext, method: String, url: String, body: String? = null): RemoteResponse }
