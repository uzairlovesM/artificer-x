package com.waheed.artificerx.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Compatibility facade; app-wide network state lives in core.runtime.NetworkManager. */
@Singleton
class NetworkManager @Inject constructor(@ApplicationContext private val context: Context) {
    fun isConnected(): Boolean = runCatching {
        val cm = context.getSystemService(android.net.ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }.getOrDefault(false)
}
