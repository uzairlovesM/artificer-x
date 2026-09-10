package com.waheed.artificerx.util

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.SingletonComponent
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext val context: Application
) {
    fun createDefaultChannel() {
        val channel = NotificationChannel(
            "artificerx_default",
            "ArtificerX Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java).createChannel(channel)
    }

    fun createHighPriorityChannel() {
        val channel = NotificationChannel(
            "artificerx_alert",
            "ArtificerX Alerts",
            NotificationManager.IMPORTANCE_HIGH
        )
        context.getSystemService(NotificationManager::class.java).createChannel(channel)
    }

    // Additional channel methods could be added here
}