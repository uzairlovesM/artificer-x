package com.waheed.artificerx.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.SingletonComponent
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkManager @Inject constructor(
    @ApplicationContext val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    // App-wide network state
    private val _isNetworkAvailable = MutableStateFlow(false)
    val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    private val _isMeteredConnection = MutableStateFlow(false)
    val isMeteredConnection: StateFlow<Boolean> = _isMeteredConnection

    fun startMonitoring() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isNetworkAvailable.value = true
                updateMeteredState()
            }

            override fun onLost(network: Network) {
                _isNetworkAvailable.value = false
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                _isNetworkAvailable.value = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _isMeteredConnection.value = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    private fun updateMeteredState() {
        val activeNetwork = connectivityManager.activeNetwork ?: return
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return
        _isMeteredConnection.value = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    fun isConnected(): Boolean = _isNetworkAvailable.value
    fun isMetered(): Boolean = _isMeteredConnection.value
}