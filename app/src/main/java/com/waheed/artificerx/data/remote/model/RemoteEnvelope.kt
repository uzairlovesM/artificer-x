package com.waheed.artificerx.data.remote.model

data class RemoteEnvelope<T>(val requestId: String, val provider: String, val model: String?, val payload: T, val receivedAtEpochMs: Long = System.currentTimeMillis())
