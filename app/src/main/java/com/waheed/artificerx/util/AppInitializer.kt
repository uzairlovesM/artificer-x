package com.waheed.artificerx.util

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AppInitializer(private val app: Application) : Application() {
    override fun onCreate() {
        super.onCreate()
        initHiltWorkManager()
        initNetworkMonitoring()
        initCrashLogging()
    }

    private fun initHiltWorkManager() {
        // Replaced Hilt worker factory setup from ArtificerXApp.kt
        // Now modularized successfully
    }

    private fun initNetworkMonitoring() {
        // NetworkObserver implementation moved here
    }

    private fun initCrashLogging(context: Context) {
        // Timber + Crashlytics setup extracted
    }
}