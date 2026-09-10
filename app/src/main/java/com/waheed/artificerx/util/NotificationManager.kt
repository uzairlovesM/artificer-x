package com.waheed.artificerx.util

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
) {
    private fun manager(): NotificationManager = context.getSystemService(NotificationManager::class.java)

    fun createDefaultChannel() {
        manager().createNotificationChannel(NotificationChannel("artificerx_default", "ArtificerX Notifications", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun createHighPriorityChannel() {
        manager().createNotificationChannel(NotificationChannel("artificerx_alert", "ArtificerX Alerts", NotificationManager.IMPORTANCE_HIGH))
    }
}
