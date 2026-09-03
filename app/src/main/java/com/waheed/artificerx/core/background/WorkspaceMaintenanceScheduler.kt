package com.waheed.artificerx.core.background

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceMaintenanceScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<WorkspaceMaintenanceWorker>(12, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("artificerx-workspace-maintenance", ExistingPeriodicWorkPolicy.KEEP, request)
    }
}
