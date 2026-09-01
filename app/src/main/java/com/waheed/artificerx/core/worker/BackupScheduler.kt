package com.waheed.artificerx.core.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules AutoBackupWorker to run roughly every 6 hours, only when
 * the device isn't in a battery-critical state (Section 137 Thermal &
 * Battery Awareness) — periodic checkpoints matter far less than not
 * draining the user's phone doing background work they didn't ask for.
 */
@Singleton
class BackupScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun scheduleAutoBackup() {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()

            val request =
                PeriodicWorkRequestBuilder<AutoBackupWorker>(6, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.LINEAR, 15, TimeUnit.MINUTES)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancelAutoBackup() {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        companion object {
            private const val WORK_NAME = "artificerx_auto_backup"
        }
    }
