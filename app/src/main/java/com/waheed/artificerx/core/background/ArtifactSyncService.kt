package com.waheed.artificerx.core.background

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.waheed.artificerx.R

class ArtifactSyncService : Service() {
    companion object {
        const val ACTION_START = "com.waheed.artificerx.action.START_SYNC"
        const val ACTION_STOP = "com.waheed.artificerx.action.STOP_SYNC"
        private const val CHANNEL_ID = "artifact_sync"
        private const val NOTIFICATION_ID = 4207
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForeground(NOTIFICATION_ID, notification("Preparing workspace synchronization"))
                return START_NOT_STICKY
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Workspace synchronization",
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = "Shows progress while ArtificerX processes background workspace data." }
            )
        }
    }

    private fun notification(text: String): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null
}
