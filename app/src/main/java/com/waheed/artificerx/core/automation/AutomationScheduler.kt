package com.waheed.artificerx.core.automation

import android.content.Context
import androidx.work.Constraints
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationScheduler @Inject constructor(@ApplicationContext private val context: Context) {
    fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<AutomationWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("artificer-automation", ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
